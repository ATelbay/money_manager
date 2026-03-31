# TODO: Review AI-generated parser configs for KZ banks

## Problem
When Gemini generates parser configs from scratch (no seed configs), the results have issues:
- **Freedom Bank**: generated config has incorrect operation names and missing/wrong category mappings
- Other KZ banks (Kaspi, Forte, Bereke, Eurasian) likely have similar issues — need to verify

## Action items
- [ ] Import a Freedom Bank PDF with empty seed configs → review the AI-generated `operation_type_map` and `category_map`
- [ ] Compare against the original hardcoded config (see git history for `default_parser_config.json`)
- [ ] Test Kaspi, Forte, Bereke, Eurasian PDFs with empty seed configs
- [ ] Decide: seed Firestore `parser_configs` collection with the 5 verified configs, or improve Gemini prompts to generate better configs
- [ ] Once verified configs exist in Firestore, the bundled `default_parser_config.json` can optionally be restored as offline fallback
