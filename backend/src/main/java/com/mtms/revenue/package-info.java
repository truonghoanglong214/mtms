/**
 * Bounded Context: Revenue.
 * Owns source catalog/NET definition metadata, source checklists, and monthly aggregate revisions.
 * Must NOT own POS orders, cart checkouts, cash/bank postings, or delivery-app settlements.
 */
package com.mtms.revenue;
