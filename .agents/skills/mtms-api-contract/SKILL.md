---
name: mtms-api-contract
description: "Defines reviewed frontend/backend REST contracts with scoped permissions, decimal/date semantics, error codes and idempotent requests."
---

# API contract handshake
1. Start from proposed `/api/v1/...` endpoints in ERD, not as already implemented API.
2. For each operation document HTTP method/path, branch scope/roles, request/response fields, examples with fictional data, errors and retry behavior.
3. Specify `Idempotency-Key` for relevant writes, If-Match/version for mutable drafts, stable `problem_code`, pagination and filter scoping.
4. Require string-safe decimal handling and business `LocalDate`; UTC timestamp explicit. Do not coerce P0 provisional P&L into payable money.
5. BE/FE owners review one contract file; QA adds negative and conflict tests. Mark as PROPOSED until signed.
**Output:** `docs/api/contracts/<TASK_ID>.md` template; version change log and contract test list.

