# MTMS — Project instructions for AI agents

**Current status: G0 NOT SIGNED.** This workspace is ready for AI configuration review; it is **not** a running Spring Boot/React application. Do not begin application feature code, database migrations, deployment, or tracker status changes until G0 is explicitly approved and the specific task plan is authorized.

## Authoritative documents, in order
1. `docs/business/MTMS_Business_Spec_Roadmap_v1_0.docx` — authoritative approved business baseline, except issues explicitly marked POLICY_VERIFY.
2. `docs/business/MTMS_Implementation_Handbook_v1_0.md` — task intent, acceptance guidance, tests and gates.
3. `docs/architecture/MTMS_ERD_Architecture_Mermaid_v1_0.md` and `docs/architecture/MTMS_PostgreSQL_Schema_Starter_v1_0.sql` — **PROPOSED** technical designs, never executable migrations.
4. `docs/planning/MTMS_Development_Tracker.xlsx` — current working tracker, dependency proposals, FE backlog; **not** owner business signoff.
5. `docs/planning/READINESS_AUDIT.md`, `docs/planning/G0_SIGNOFF_CHECKLIST.md` and gap/dependency records — unresolved readiness actions.

When sources disagree, cite their BR/ADR/Task/TC IDs and report the contradiction. Never silently invent or replace a business decision. Original files are unchanged except for normalized filenames.

## Domain and scope guards
- **NO POS**: no checkout, beverage order/cart, individual customer payments or automatic per-cup ingredient deduction.
- Daily revenue is aggregated. Delivery-app NET is recorded by business date, **not** automatically as bank funds.
- `business_entity` and `branch` differ; financial entitlement does not confer Branch Owner software access.
- Every sensitive action uses server-side entity/branch/action/row-scoped authorization.
- Inventory is manually posted with per-branch moving WAC; never consume expired lots or allow negative available stock.
- All money/quantities use exact decimal math. Time is business local date versus UTC event timestamp.
- Approved payable hours are not GPS timestamps; payroll expense belongs to service month; no self-approval.
- P0 managerial P&L must display **TẠM TÍNH / PROVISIONAL**. No real investor payouts until G5 specialist verification.
- Immutable postings, idempotency keys, transaction/locking, auditable approved corrections and private evidence are required.

## Gates and execution
- **Before G0:** analysis, task preparation, documentation review and AI-config validation ONLY. Never create app features, migrations, real bank operations or mark tasks complete.
- **After G0:** for each task, propose a scoped plan, get explicit user approval, check dependencies, implement narrowly, execute tests, independent QA, gather evidence, request acceptance, then update tracker/commit with authorization.
- **Never** run destructive file/database operations without explicit per-action approval. Never claim tests passed when not run.
- Do not reinterpret preparation of this repository folder as G0 or a task implementation approval.

## Coordination
- Primary Agent: `mtms-tech-lead`.
- Specialists: `mtms-business-analyst`, `mtms-architect`, `mtms-backend-engineer`, `mtms-frontend-engineer`, `mtms-qa-reviewer`.
- Use `.agents/skills/` and `.agents/rules/` as task-relevant guidance. Avoid competing edits to the same migration/API contract.
- Begin with `docs/ai/START_HERE.md` and check `docs/planning/CURRENT_STATE.md`.
- New files must be integrated using `docs/ai/HOW_TO_ADD_FILES.md`; preserve user code and current migrations.
