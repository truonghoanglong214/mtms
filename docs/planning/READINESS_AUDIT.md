# MTMS v1.1 — readiness audit & remediation

**Working artifact prepared:** 2026-11-09. **Original baseline:** 2026-11-07 (v1.0).  
**Status:** DRAFT FOR BUSINESS/TECH LEAD APPROVAL — NOT G0 SIGNED. No source code, DB migration, API or test has been executed.

## Verified source inventory
- 5 files reviewed: business requirements DOCX, 104-task XLSX, architecture Mermaid, implementation handbook, SQL starter.
- **104 task IDs**, all unique: P0 52 / P1 44 / P2 8; all `Not Started`.
- **70** requirements; **56** test cases; **31** ADRs; **20** risks; **G0–G7** all `Not Started`.
- Source Task discipline: **93 Backend**, **11 Backend+Frontend**. FE backlog is therefore an *additional proposed workstream*, not 70+ new approved requirements.
- Original dependency graph references only existing IDs and is acyclic; it is nevertheless incomplete for business prerequisites.
- Original Task references omit five BRs and three TCs. v1.1 working copy safely links 3 BRs and 3 TCs; **BR-FIN-08** and **BR-CAP-10** remain open for explicit handling.
- The footer of the v1.0 Business Spec reports **51 tests**, while the tracker has **56**. Correction requested for a future approved Business Spec revision; v1.0 DOCX is preserved unchanged.

## Non-negotiable business boundaries
- **NO POS.** No customer checkout, per-beverage orders, or recipe-based automatic stock deduction.
- Daily revenue is an aggregate; delivery-app net revenue is **not** an automatic real bank deposit.
- Managerial P&L in P0 is **provisional**; no real profit distribution until P1/G5 policy signoff.
- Separate registered entity from branch, and separate investor economic entitlements from software Owner privileges.
- Require branch- and row-scoped backend authorization, immutable/auditable money and stock postings, idempotency, and controlled period corrections.

## Gap register (actionable)
- **GAP-01 — Opening balance capture not owned end-to-end** (Critical, G0 review): Import staging and opening inventory exist, but cash/bank/payables/receivables/assets opening snapshot needs explicit end-to-end acceptance and ownership. **Action:** Add scoped implementation subtask and verified balance reconciliation flow; no invented historical imports.
- **GAP-02 — External legal agreement signoff not a software task** (High, G5 blocked until verified): Legal verification requirement is currently not a direct Task link; treat as G5 manual release gate, not automated coding. **Action:** Map to G5 checklist; collect documented signoff from responsible professional.
- **GAP-03 — Frontend ownership/acceptance insufficiently itemized** (High, G0 review): 93 backend-only parent tasks despite many screens and mobile interactions. **Action:** Review FE backlog; approve screens/contract/e2e scope, estimate separately.
- **GAP-04 — Underspecified dependency DAG** (High, G0 review): Valid IDs and acyclic does not mean all business prerequisites are modeled. **Action:** Review dependency patches, maintain topological order, re-check after approval.
- **GAP-05 — Cross-branch relational consistency needs enforcement** (High, Before G1): Separate foreign keys to branch and referenced entity do not prove both belong to same branch; this applies to money and stock. **Action:** Define compound-keys/constraints or transaction validation plus negative integration tests.
- **GAP-06 — Payout retry and historical attempt semantics** (High, Before G5): One payment per line vs retry failure history requires explicit payment attempt/recovery design. **Action:** Add failure/pending/reversal state flow before G5; test concurrent repeat.
- **GAP-07 — Business spec footer says 51 tests** (Medium, G0 review): Actual Test Matrix contains 56 cases; summary text is stale. **Action:** Update next approved Business Spec revision; source v1.0 remains unchanged.
- **GAP-08 — G0 is not signed** (High, G0 pending): All 8 gates Not Started; no implementation evidence to support Done. **Action:** Owner accepts baseline and unresolved policy flags; record approval evidence.
- **GAP-09 — Provisional P&L must remain visibly provisional** (High, Before G3): P0 deliberately lacks depreciation, legal/tax validation and cash payout. **Action:** Show prominent provisional label; prevent production distribution in P0.
- **GAP-10 — Three important tests unreferenced in source Tracker** (Medium, G0 review): TC-014/031/037 existed but were not linked directly from tasks. **Action:** Working copy links tests to actual implementation tasks.

## Decisions at this stage
1. **No change** to business decisions, approved rates, legal policy or source schema was automatically approved.
2. Edits in derived Tracker v1.1 are marked **working copy — G0 review**. Do not treat them as owner signoff.
3. Agent/Skill configuration files now exist, but their loading on the user machine and any application build remain unverified. CI and application code are not yet implemented.
4. Estimates from v1.0 (P0 590h, P1 630h, P2 112h) are historical planning estimates; FE subtasks do not silently change these totals.

## Acceptance of baseline readiness
- [x] Keep original source files immutable and hashed in source manifest.
- [x] Verify task IDs and initial dependency DAG; propose missing edges.
- [x] Produce requirement/test coverage and outstanding gaps.
- [x] Produce FE backlog aligned to original parent tasks and roles.
- [x] Produce SQL/static architecture review, G0 checklist and handoff.
- [ ] User/Owner accepts candidate task/BR/test links and dependency working copy.
- [ ] User/Owner confirms FE MVP screen scope and estimates.
- [ ] Business owner formally signs G0; unresolved policy requirements recorded as blocked.

**Read first:** `docs/planning/G0_SIGNOFF_CHECKLIST.md`, then `docs/ai/ANTIGRAVITY_HANDOFF.md`.
