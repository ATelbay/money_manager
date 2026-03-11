import * as admin from "firebase-admin";
import { onDocumentUpdated, onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";

const DEFAULT_THRESHOLD = 3;

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

async function getPromotionThreshold(): Promise<number> {
  try {
    const remoteConfig = admin.remoteConfig();
    const template = await remoteConfig.getTemplate();
    const param = template.parameters["promotion_threshold"];
    if (param?.defaultValue && "value" in param.defaultValue) {
      return parseInt(param.defaultValue.value, 10) || DEFAULT_THRESHOLD;
    }
  } catch (e) {
    logger.warn("Failed to read promotion_threshold from Remote Config, using default", e);
  }
  return DEFAULT_THRESHOLD;
}

async function promoteCandidate(candidateId: string, candidate: ParserCandidate): Promise<void> {
  if (candidate.status !== "candidate") {
    logger.info(`Candidate ${candidateId} status is ${candidate.status}, skipping`);
    return;
  }

  const threshold = await getPromotionThreshold();
  if (candidate.successCount < threshold) {
    logger.info(`Candidate ${candidateId} successCount=${candidate.successCount} < threshold=${threshold}`);
    return;
  }

  logger.info(`Promoting candidate ${candidateId} (bank=${candidate.bankId}, successCount=${candidate.successCount})`);

  // Read current parser_configs from Remote Config
  const remoteConfig = admin.remoteConfig();
  const template = await remoteConfig.getTemplate();
  const parserConfigsParam = template.parameters["parser_configs"];

  const db = admin.firestore();
  const candidateRef = db.collection("parser_candidates").doc(candidateId);

  let configList: ParserConfigList = { banks: [] };
  if (parserConfigsParam?.defaultValue && "value" in parserConfigsParam.defaultValue) {
    try {
      configList = JSON.parse(parserConfigsParam.defaultValue.value);
    } catch (e) {
      logger.error("Failed to parse existing parser_configs — aborting to prevent config loss", e);
      await candidateRef.update({ status: "error", updatedAt: Date.now() });
      return;
    }
  }

  // Check for duplicate (same bankId + transactionPattern already promoted)
  const isDuplicate = configList.banks.some(
    (bank: Record<string, unknown>) =>
      bank["bank_id"] === candidate.bankId &&
      bank["transaction_pattern"] === candidate.transactionPattern
  );

  if (isDuplicate) {
    logger.warn(`Candidate ${candidateId} conflicts with existing config, rejecting`);
    await candidateRef.update({ status: "rejected", updatedAt: Date.now() });
    return;
  }

  // Parse the candidate's config and append
  let newConfig: Record<string, unknown>;
  try {
    newConfig = JSON.parse(candidate.parserConfigJson);
  } catch (e) {
    logger.error(`Failed to parse parserConfigJson for candidate ${candidateId}`, e);
    await candidateRef.update({ status: "rejected", updatedAt: Date.now() });
    return;
  }

  // --- Server-side validation before Remote Config publish ---

  // 1. Schema validation — required ParserConfig fields must exist
  const requiredFields = ["bank_id", "bank_markers", "transaction_pattern", "date_format", "amount_format"];
  const missingFields = requiredFields.filter((f) => !(f in newConfig));
  if (missingFields.length > 0) {
    logger.error(`Candidate ${candidateId} missing required fields: ${missingFields.join(", ")}`);
    await candidateRef.update({ status: "rejected", updatedAt: Date.now() });
    return;
  }

  // 2. Field consistency — bank_id and transaction_pattern must be non-empty strings
  if (typeof newConfig["bank_id"] !== "string" || (newConfig["bank_id"] as string).trim() === "") {
    logger.error(`Candidate ${candidateId} has empty or non-string bank_id`);
    await candidateRef.update({ status: "rejected", updatedAt: Date.now() });
    return;
  }
  if (typeof newConfig["transaction_pattern"] !== "string" || (newConfig["transaction_pattern"] as string).trim() === "") {
    logger.error(`Candidate ${candidateId} has empty or non-string transaction_pattern`);
    await candidateRef.update({ status: "rejected", updatedAt: Date.now() });
    return;
  }

  // 3. Regex syntax validation — pattern must be a valid RegExp
  const txPattern = newConfig["transaction_pattern"] as string;
  try {
    new RegExp(txPattern);
  } catch (e) {
    logger.error(`Candidate ${candidateId} has invalid regex in transaction_pattern: ${txPattern}`, e);
    await candidateRef.update({ status: "rejected", updatedAt: Date.now() });
    return;
  }

  // 4. ReDoS safety — reject patterns with nested quantifiers like (a+)+, (a*)*,  (a+)*, (a*)+
  const nestedQuantifierPattern = /(\((?:[^()]*[+*])[^()]*\))[+*{]/;
  if (nestedQuantifierPattern.test(txPattern)) {
    logger.error(`Candidate ${candidateId} rejected: transaction_pattern contains nested quantifiers (ReDoS risk): ${txPattern}`);
    await candidateRef.update({ status: "rejected", updatedAt: Date.now() });
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
  await candidateRef.update({ status: "promoted", updatedAt: Date.now() });
  logger.info(`Candidate ${candidateId} promoted successfully`);
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
