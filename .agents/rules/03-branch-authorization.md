---
trigger: model_decision
description: "Apply when implementing or reviewing branch-scoped authorization, data access, ownership and roles"
---

# Branch security checklist
- Always resolve the principal's entity + branch membership and action permission on the backend.
- Scope every query by branch/entity where appropriate; reject cross-branch resource IDs and count/lookup leaks.
- A global admin exception must be explicit, tested and documented. Delegated Owner authority must be effective-dated.
- Investors get separately granted report visibility; no payroll, GPS or evidence by default.
- Include 401/403/404 negative tests for branch and row isolation.
