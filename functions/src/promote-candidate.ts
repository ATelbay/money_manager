import * as admin from "firebase-admin";
import { onDocumentUpdated, onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";

const DEFAULT_THRESHOLD = 3;

const STATUS = {
  CANDIDATE: "candidate",
  PROMOTED: "promoted",
  REJECTED: "rejected",
  ERROR: "error",
} as const;

interface ParserCandidate {
  bankId: string;
  transactionPattern: string;
  parserConfigJson: string;
  anonymizedSample: string;
  userIdHash: string;
  successCount: number;
  status: string;
  createdAt: number;
  updatedAt: number;
}

interface ParserConfigList {
  banks: Record<string, unknown>[];
}

// ReDoS safety — reject patterns with nested quantifiers like (a+)+, (a*)*,  (a+)*, (a*)+
const nestedQuantifierPattern = /(\((?:[^()]*[+*])[^()]*\))[+*{]/;

async function promoteCandidate(candidateId: string, candidate: ParserCandidate): Promise<void> {
  const db = admin.firestore();
  const candidateRef = db.collection("parser_candidates").doc(candidateId);

  // Fetch Remote Config template once upfront
  const remoteConfig = admin.remoteConfig();
  let template: admin.remoteConfig.RemoteConfigTemplate;
  try {
    template = await remoteConfig.getTemplate();
  } catch (e) {
    logger.error("Failed to fetch Remote Config template", e);
    return;
  }

  // Extract promotion threshold inline
  let threshold = DEFAULT_THRESHOLD;
  try {
    const param = template.parameters["promotion_threshold"];
    if (param?.defaultValue && "value" in param.defaultValue) {
      threshold = parseInt(param.defaultValue.value, 10) || DEFAULT_THRESHOLD;
    }
  } catch (e) {
    logger.warn("Failed to read promotion_threshold from Remote Config, using default", e);
  }

  // Read current parser_configs
  const parserConfigsParam = template.parameters["parser_configs"];
  let configList: ParserConfigList = { banks: [] };
  if (parserConfigsParam?.defaultValue && "value" in parserConfigsParam.defaultValue) {
    try {
      configList = JSON.parse(parserConfigsParam.defaultValue.value);
    } catch (e) {
      logger.error("Failed to parse existing parser_configs — aborting to prevent config loss", e);
      await candidateRef.update({ status: STATUS.ERROR, updatedAt: Date.now() });
      return;
    }
  }

  // Wrap read-check-promote-write in a Firestore transaction to prevent race conditions
  await db.runTransaction(async (transaction) => {
    const snap = await transaction.get(candidateRef);
    const data = snap.data() as ParserCandidate | undefined;

    if (!data) {
      logger.warn(`Candidate ${candidateId} no longer exists`);
      return;
    }

    if (data.status !== STATUS.CANDIDATE) {
      logger.info(`Candidate ${candidateId} status is ${data.status}, skipping`);
      return;
    }

    if (data.successCount < threshold) {
      logger.info(`Candidate ${candidateId} successCount=${data.successCount} < threshold=${threshold}`);
      return;
    }

    logger.info(`Promoting candidate ${candidateId} (bank=${data.bankId}, successCount=${data.successCount})`);

    // Check for duplicate (same bankId + transactionPattern already promoted)
    const isDuplicate = configList.banks.some(
      (bank: Record<string, unknown>) =>
        bank["bank_id"] === data.bankId &&
        bank["transaction_pattern"] === data.transactionPattern
    );

    if (isDuplicate) {
      logger.warn(`Candidate ${candidateId} conflicts with existing config, rejecting`);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }

    // Parse the candidate's config and append
    let newConfig: Record<string, unknown>;
    try {
      newConfig = JSON.parse(data.parserConfigJson);
    } catch (e) {
      logger.error(`Failed to parse parserConfigJson for candidate ${candidateId}`, e);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }

    // --- Server-side validation before Remote Config publish ---

    // 1. Schema validation — required ParserConfig fields must exist
    const requiredFields = ["bank_id", "bank_markers", "transaction_pattern", "date_format", "amount_format"];
    const missingFields = requiredFields.filter((f) => !(f in newConfig));
    if (missingFields.length > 0) {
      logger.error(`Candidate ${candidateId} missing required fields: ${missingFields.join(", ")}`);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }

    // 2. Field consistency — bank_id and transaction_pattern must be non-empty strings
    if (typeof newConfig["bank_id"] !== "string" || (newConfig["bank_id"] as string).trim() === "") {
      logger.error(`Candidate ${candidateId} has empty or non-string bank_id`);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }
    if (typeof newConfig["transaction_pattern"] !== "string" || (newConfig["transaction_pattern"] as string).trim() === "") {
      logger.error(`Candidate ${candidateId} has empty or non-string transaction_pattern`);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }

    // 3. bank_markers must be a non-empty array of non-empty strings
    const bankMarkers = newConfig["bank_markers"];
    if (
      !Array.isArray(bankMarkers) ||
      bankMarkers.length === 0 ||
      bankMarkers.some((m) => typeof m !== "string" || (m as string).trim() === "")
    ) {
      logger.error(`Candidate ${candidateId} has invalid bank_markers: must be a non-empty array of non-empty strings`);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }

    // 4. skip_patterns — if present, each entry must be a valid RegExp and pass ReDoS check
    if ("skip_patterns" in newConfig) {
      const skipPatterns = newConfig["skip_patterns"];
      if (!Array.isArray(skipPatterns)) {
        logger.error(`Candidate ${candidateId} skip_patterns must be an array`);
        transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
        return;
      }
      for (const sp of skipPatterns) {
        if (typeof sp !== "string") {
          logger.error(`Candidate ${candidateId} skip_patterns entries must be strings`);
          transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
          return;
        }
        try {
          new RegExp(sp);
        } catch (e) {
          logger.error(`Candidate ${candidateId} has invalid regex in skip_patterns: ${sp}`, e);
          transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
          return;
        }
        if (nestedQuantifierPattern.test(sp)) {
          logger.error(`Candidate ${candidateId} rejected: skip_patterns entry contains nested quantifiers (ReDoS risk): ${sp}`);
          transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
          return;
        }
      }
    }

    // 5. Regex syntax validation — transaction_pattern must be a valid RegExp
    const txPattern = newConfig["transaction_pattern"] as string;
    try {
      new RegExp(txPattern);
    } catch (e) {
      logger.error(`Candidate ${candidateId} has invalid regex in transaction_pattern: ${txPattern}`, e);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }

    // 6. ReDoS safety for transaction_pattern
    if (nestedQuantifierPattern.test(txPattern)) {
      logger.error(`Candidate ${candidateId} rejected: transaction_pattern contains nested quantifiers (ReDoS risk): ${txPattern}`);
      transaction.update(candidateRef, { status: STATUS.REJECTED, updatedAt: Date.now() });
      return;
    }

    // --- End validation ---

    configList.banks.push(newConfig);

    // Publish to Remote Config
    if (!parserConfigsParam) {
      template.parameters["parser_configs"] = {
        defaultValue: { value: JSON.stringify(configList) },
      };
    } else {
      parserConfigsParam.defaultValue = { value: JSON.stringify(configList) };
    }

    try {
      await remoteConfig.publishTemplate(template);
      logger.info(`Published updated parser_configs with ${configList.banks.length} banks`);
    } catch (e) {
      logger.error("Failed to publish Remote Config template", e);
      return; // Leave as candidate for retry
    }

    // Mark as promoted
    transaction.update(candidateRef, { status: STATUS.PROMOTED, updatedAt: Date.now() });
    logger.info(`Candidate ${candidateId} promoted successfully`);
  });
}

export const onParserCandidateUpdated = onDocumentUpdated(
  "parser_candidates/{candidateId}",
  async (event) => {
    const candidateId = event.params.candidateId;
    const data = event.data?.after.data() as ParserCandidate | undefined;
    if (!data) {
      logger.warn(`No data for candidate ${candidateId}`);
      return;
    }
    await promoteCandidate(candidateId, data);
  }
);

export const onParserCandidateCreated = onDocumentCreated(
  "parser_candidates/{candidateId}",
  async (event) => {
    const candidateId = event.params.candidateId;
    const data = event.data?.data() as ParserCandidate | undefined;
    if (!data) {
      logger.warn(`No data for candidate ${candidateId}`);
      return;
    }
    await promoteCandidate(candidateId, data);
  }
);
