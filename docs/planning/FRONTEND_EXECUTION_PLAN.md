# Proposed Frontend workstream for MTMS

## Why it exists
Source `Tasks` are predominantly labelled Backend (93/104), although business flows require responsive interfaces. This is a **delivery decomposition**, not an approved scope expansion or a POS module.

## Principles
- FE task is linked to an existing source Task (`Parent task`); parent task remains the authoritative business scope.
- The FE backlog uses proposed screens and acceptance criteria; owner confirms before work begins. All estimates are `TBD`, not silently added to 1,332 hours.
- Backend enforces all access; frontend role guards are UX only. Show the current branch, transaction status, date and reversible/immutable states.
- UI must distinguish *provisional profit*, *actual cash*, *delivery-app net revenue*, *unpaid payroll*, and *approved-but-unpaid investor distribution*.
- Mobile-first particularly for Manager/Employee; desktop-first high-density reporting for Owner/Admin. Accessibility and empty/loading/error/denied states are part of Done.
- Prefer reusable design system (tables, forms, status badges, confirmation dialogs, permission empty states, number/date formatting) without distracting animations on financial actions.

## Proposed backlog summary
- P0: **32 FE work items**, linked to existing parent tasks.
- P1: **33 FE work items**, linked to existing parent tasks.
- P2: **4 FE work items**, linked to existing parent tasks.

## Required frontend contract per slice
Each task is blocked from full integration until a reviewed contract contains: endpoint/method, request/response DTOs, pagination/filtering, authorization scopes, error/problem codes, idempotency/ETag strategy for write actions, loading states, and exact business date/money semantics.

## Suggested first FE vertical slice
1. `FE-P0-01` responsive shell and design tokens; `FE-P0-02` login/session state.
2. `FE-P0-03` branch scope selector, then Admin branch onboarding.
3. Business day calendar + daily aggregate revenue entry + history.
4. Reconciliation UI for the same report (delivery-app net ≠ cash bank).
5. Integrate P&L as **provisional** after finance, payroll and inventory streams are tested.

## FE acceptance and release proof
Test role-based UI, keyboard navigation, mobile minimum widths, field validation, timeout and retry UX, navigation fallback, locale formatting and 403/404 empty states. Save screenshots/videos with **fictional data** and attach to parent Task evidence.

See `FE_Backlog_Proposed_v1_1.csv` and the `FE Backlog` worksheet for line-level details.
