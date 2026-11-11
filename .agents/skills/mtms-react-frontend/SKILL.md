---
name: mtms-react-frontend
description: "Plans and implements approved React TypeScript Vite web interfaces for MTMS with mobile employee flows and accurate finance status UI."
---

# React frontend
**Before G0/task approval:** write screen plans and contracts only; no scaffolding.
1. Find FE-P0/FE-P1 child in FE backlog and parent business task.
2. Design responsive page/component list by role, tasks and required data. Prefer accessible reusable forms/tables/state badges.
3. Review business contract, permission scope, 400/401/403/404/409/422, loading/empty/error and request retry UX.
4. Make source-of-truth distinctions visible (app net vs bank money, profit provisional, unpaid liability, OPEN/CLOSED day).
5. After approval: implement React TS and test keyboard, responsive, error and mocked contract fidelity, then E2E against real API.
6. Avoid heavy animations for finance or operational forms; Taste/GSAP are optional UI aids, not domain logic.
**Output:** screen/component map, FE subtask link, build/test evidence and screenshots using fictional data.

