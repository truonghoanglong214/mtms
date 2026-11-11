---
name: mtms-test-review
description: "Reviews MTMS implementation evidence against BR/Task/TC, authorization, concurrency, security and business-state correctness."
---

# Test & review
1. Read task BR/TC, criteria and diff. Distinguish review-only from executed tests.
2. Verify unit, integration, API and E2E coverage appropriate to the feature.
3. Security: negative branch/row/report authorization; documents private, sessions/logout correct.
4. Finance: duplicate retries, app-net not posted to bank, separate liabilities; stock: WAC/expiry/negative/concurrent issues; payroll: approved hours/effective rates; close: missing report and immutable revision.
5. Run only safe approved tests; note exact command, exit code and log path. Do not report PASS if not executed.
6. Return severity-tagged defects, missing evidence and a GO/BLOCKED recommendation; final acceptance belongs to owner.
**Output:** test matrix with Pass/Fail/Not Run, actual evidence refs, explicit regressions.

