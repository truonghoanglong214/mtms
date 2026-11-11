---
trigger: always_on
description: "Prevent unsafe financial postings and evidence exposure"
---

# Financial and data safety
- Revenue, profit, actual cash/bank, expense accrual, payables, investor capital and distributions are distinct concepts.
- Transactions posting money/stock must be idempotent and concurrency-safe; audit immutable source links.
- No real data/secrets, no payout, no irreversible changes, no migration in absence of the corresponding gates.
- Treat P0 managerial P&L as provisional; G5 controls real profit distribution.
