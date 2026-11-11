---
trigger: model_decision
description: "Apply when handling financial evidence, employee GPS, payroll, investor identities, bank data and file uploads"
---

# Privacy and audit
- Test only with fictional seed data. No real CSV, Zalo/Excel personal information or bank credentials in repo/tests.
- Evidence files live in private storage; verify authorization before any short-lived link; avoid public buckets.
- Audit sensitive approvals/changes with traceable actor, timestamp and reason while redacting sensitive payloads.
- For AI/OCR P2, suggestions require human review and no direct writes to approved finance tables.
