-- P0-INV-06 / P0-INV-07 / BR-INV-01 / BR-INV-02 / TC-020 / TC-021
-- Create indexes for inventory reporting, movement ledger, and COGS read model queries.

CREATE INDEX idx_stock_movements_cogs_lookup ON mtms.stock_movements (branch_id, movement_type, business_date);
CREATE INDEX idx_stock_balances_valuation ON mtms.stock_balances (branch_id, inventory_value DESC);
