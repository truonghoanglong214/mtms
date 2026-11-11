---
trigger: always_on
description: "Block coding until explicit signoff and task-level approvals"
---

# Execution gate / governance
- G0 remains **Not Started**. Until explicit recorded approval: planning, codebase inspection and local static validation only.
- Do not create feature code, apply migration, commit, deploy or update Tracker status.
- Even after G0: a reviewed Task plan requires explicit approval before implementation.
- Task Done requires verified code, acceptance criteria, automated evidence and user acceptance. Test NOT RUN is never a pass.
- Never overwrite the immutable five original baseline v1.0 documents.
