# G0 — Requirements & AI execution baseline signoff

**Current state: G0 SIGNED / APPROVED on 2026-11-13.** The Owner explicitly approved `G0-01` through `G0-10` for Phase 0 Foundation. Outstanding `POLICY_VERIFY` items remain release-specific gates and are not treated as specialist verification.

| ID | Required signoff/check | Who | Status | Evidence expected |
|---|---|---|---|---|
| G0-01 | Confirm NO POS, daily aggregate sales only, no recipe auto-consumption | Business Owner | **APPROVED — 2026-11-13** | Owner full G0 signoff message; original BR/ADR-001 boundary accepted |
| G0-02 | Approve v1.0 business baseline as authoritative requirements | Business Owner | **APPROVED — 2026-11-13** | Owner full G0 signoff message |
| G0-03 | Review 19 dependency proposals, especially P&L and close prerequisites | Tech Lead + Owner | **APPROVED — 2026-11-13** | Owner full G0 signoff message; working dependency proposals accepted for Phase 0 planning |
| G0-04 | Resolve `BR-FIN-08`: owner + scope + acceptance for complete opening balances | Owner + Finance | **APPROVED — 2026-11-13** | Owner full G0 signoff message; implementation remains task-scoped with its own acceptance/tests |
| G0-05 | Record `BR-CAP-10` as G5 external verification, not software auto-approval | Owner + qualified adviser | **APPROVED — 2026-11-13** | Owner accepted G5 external-verification gate; no specialist compliance claim made |
| G0-06 | Accept source-to-Task mappings of `BR-HR-04`, `BR-FIN-03`, `BR-CAP-06` and TC-014/031/037 | Tech Lead | **APPROVED — 2026-11-13** | Owner full G0 signoff message; mapping accepted for Phase 0 baseline |
| G0-07 | Confirm FE screen priority, parent IDs and separate FE effort estimate | Owner + FE Lead | **APPROVED — 2026-11-13** | Owner full G0 signoff message; FE work still needs task-specific approval |
| G0-08 | Accept proposed stack, database migration strategy and security boundaries | Tech Lead | **APPROVED — 2026-11-13** | Owner approval message for `P0-ORG-02`; ADR-025/database migration direction accepted for this task |
| G0-09 | Preserve outstanding `POLICY_VERIFY` flags and block payout before G5 | Owner | **APPROVED — 2026-11-13** | Owner full G0 signoff message; G5 remains blocked pending qualified verification |
| G0-10 | Approve use of fictional seed data only; secrets/private files never committed | Tech Lead + Owner | **APPROVED — 2026-11-13** | Owner full G0 signoff message; fictional-data/secrets guard accepted |

## Decision states
- **APPROVED**: Owner recorded explicit evidence of the decision; do not infer from a generated artifact.
- **PROPOSED**: Engineering suggestion for discussion; not permission to change business behavior.
- **POLICY_VERIFY**: Qualified accounting/legal/labor verification required before release of relevant feature.
- **BLOCKED**: Task or gate missing prerequisite approval or test evidence.

G0 was explicitly approved on 2026-11-13. This authorizes only separately approved Phase 0 tasks; each task still requires scoped implementation, test evidence, independent QA and Owner acceptance before Tracker completion.
