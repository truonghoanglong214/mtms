# G0 — Requirements & AI execution baseline signoff

**Current state: NOT STARTED.** Checklists below are proposed; no box represents a recorded owner signature.

| ID | Required signoff/check | Who | Status | Evidence expected |
|---|---|---|---|---|
| G0-01 | Confirm NO POS, daily aggregate sales only, no recipe auto-consumption | Business Owner | Pending | Dated approval of original BR/ADR-001 |
| G0-02 | Approve v1.0 business baseline as authoritative requirements | Business Owner | Pending | Signoff reference |
| G0-03 | Review 19 dependency proposals, especially P&L and close prerequisites | Tech Lead + Owner | Pending | Approved tracker revision |
| G0-04 | Resolve `BR-FIN-08`: owner + scope + acceptance for complete opening balances | Owner + Finance | Pending | Task linkage and test set |
| G0-05 | Record `BR-CAP-10` as G5 external verification, not software auto-approval | Owner + qualified adviser | Pending | Policy verification plan |
| G0-06 | Accept source-to-Task mappings of `BR-HR-04`, `BR-FIN-03`, `BR-CAP-06` and TC-014/031/037 | Tech Lead | Pending | Signed traceability table |
| G0-07 | Confirm FE screen priority, parent IDs and separate FE effort estimate | Owner + FE Lead | Pending | Approved FE backlog |
| G0-08 | Accept proposed stack, database migration strategy and security boundaries | Tech Lead | Pending | ADR-025 review and architecture decision |
| G0-09 | Preserve outstanding `POLICY_VERIFY` flags and block payout before G5 | Owner | Pending | Decision register |
| G0-10 | Approve use of fictional seed data only; secrets/private files never committed | Tech Lead + Owner | Pending | Repo security rule signoff |

## Decision states
- **APPROVED**: Owner recorded explicit evidence of the decision; do not infer from a generated artifact.
- **PROPOSED**: Engineering suggestion for discussion; not permission to change business behavior.
- **POLICY_VERIFY**: Qualified accounting/legal/labor verification required before release of relevant feature.
- **BLOCKED**: Task or gate missing prerequisite approval or test evidence.

Once G0 is explicitly approved, update `Release Gates` to reflect real evidence. **Do not mark G0 Done merely because baseline readiness documentation was prepared.**
