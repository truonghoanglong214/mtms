# Agent handoff contract — MTMS

## Input to delegate
Every assignment must include: `Parent Task ID`, selected `FE child` if any, `G0/Gate status`, `BR`, `TC`, `ADR/POLICY_VERIFY`, approved scope, dependencies verified, allowed file paths, forbidden paths and expected artifact.

## Allowed stages
1. **REVIEW_ONLY** (now): read docs, propose risks/plan, never change source or Tracker.
2. **PLAN_PENDING_APPROVAL**: after recorded G0, prepare a bounded plan for one Task; no code until approved.
3. **IMPLEMENT_AUTHORIZED**: explicit user approval for Task/allowed paths; code plus tests.
4. **QA_AND_ACCEPTANCE**: independent QA evidence, user acceptance, then propose status/commit.

## Delegation strategy
- Tech Lead owns task scope and integration schedule.
- BA reads approved requirements and resolves neither policy nor taxes by assumption.
- Architect negotiates API/domain boundary; one shared contract owner per slice.
- BE owns backend module and migration changes; no concurrent writer for same database objects.
- FE owns React component/page files; may work on mockable UI only after accepted API contract and implementation approval.
- QA works from a separate review scope; final release approval belongs to user.

## Response contract
```text
TASK: ...
MODE: REVIEW_ONLY | PLAN_PENDING_APPROVAL | IMPLEMENT_AUTHORIZED | QA_AND_ACCEPTANCE
SOURCES: BR-..., P0-..., TC-..., ADR-...
CURRENT GATE: ...
SCOPE / OWNERSHIP: ...
DEPENDENCY STATUS: ...
PLAN / ACTIONS: ...
ACTUAL TESTS: PASS | FAIL | NOT RUN (commands, logs, exit codes)
RISKS / POLICY_VERIFY: ...
DIFF / FILES CHANGED: ...
DECISION REQUIRED: ...
STATUS RECOMMENDATION: BLOCKED | READY_FOR_REVIEW | ACCEPTED_BY_OWNER
```
Do not claim a real agent performed work when only a static document was generated.
