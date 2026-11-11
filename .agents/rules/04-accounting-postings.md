---
trigger: model_decision
description: "Apply when planning revenue, finance, payroll accrual, ledger, monthly close or investor distribution work"
---

# Accounting/posting constraints
- Every posted cash/stock event has unique source type/id/leg and idempotency semantics.
- Confirmed daily delivery net must not automatically increase bank balance; settlements occur later and do not duplicate beverage sales.
- Receipt not yet consumed changes inventory and payable, not ingredient COGS. Payroll expense follows service month, not payment month.
- No silent recomputation of closed-month P&L or WAC. Use revisions/reopen with audit and impact analysis.
- Actual distributions stay behind G5 verified conditions; full-only successful payout and retry semantics must be separately designed.
