/**
 * Bounded Context: Inventory.
 * Owns catalog, receipts, manual consumption documents, lot tracking, physical counts, and moving WAC valuation.
 * Must NOT allow negative available stock or POS recipe-driven auto consumption.
 */
package com.mtms.inventory;
