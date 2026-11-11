---
trigger: model_decision
description: "Apply when planning or implementing manual stock movements, receipts, issues, lots and WAC"
---

# Inventory discipline
- Base unit conversions are snapshotted on receipts; WAC is per branch/item, not per lot.
- Lock relevant balance rows and post movement+balance change atomically in one transaction.
- Prevent negative available stock; lot expiry limits consumption; allow approved disposal with audit.
- Separate intra-entity transfers from inter-entity loans/material sales; reconcile received actual quantity.
- Test simultaneous issues, partial receipt, backdated adjustment and numeric rounding boundaries.
