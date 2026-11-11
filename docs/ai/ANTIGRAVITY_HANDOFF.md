# MTMS — Agent handoff protocol

## Files to consult at the start
1. `AGENTS.md` and `docs/planning/CURRENT_STATE.md`.
2. `docs/planning/READINESS_AUDIT.md` and `G0_SIGNOFF_CHECKLIST.md`.
3. `docs/planning/MTMS_Development_Tracker.xlsx`, `DEPENDENCY_REVIEW.md`, `FRONTEND_EXECUTION_PLAN.md`.
4. `docs/business/` original business documents, `docs/architecture/` ERD + conceptual SQL + review.
5. `docs/ai/AGENT_SKILL_MATRIX.md`, `AGENT_HANDOFF_CONTRACT.md` and templates.

## Handoff rules
- Before G0: review/plan only; no implementation delegation.
- After G0 and per-task user approval, Tech Lead delegates non-overlapping work to Backend, Frontend, Architect, BA, QA.
- Each handoff includes: Task ID; exact BR/TC/ADR; files in-scope; relevant dependencies; API/schema contract; tests; real observations and blockers.
- Never let two Agents change the same migration or API contract concurrently.
- QA reports actual test status including NOT RUN. No task is Done without user acceptance and evidence.

## First planned task
`P0-ORG-01` (Spring Boot + React foundation), documented in `docs/planning/FIRST_TASK_P0_ORG_01.md`. G0 is currently NOT SIGNED. Use `docs/ai/prompts/03_P0_ORG_01_PLAN.md` only when permitted.
