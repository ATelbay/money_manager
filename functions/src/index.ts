import * as admin from "firebase-admin";

admin.initializeApp();

export { onParserCandidateUpdated, onParserCandidateCreated } from "./promote-candidate";
