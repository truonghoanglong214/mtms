/**
 * Bounded Context: Closing.
 * Owns period status evaluation, immutable closing snapshots, and correction references.
 * Must NOT rewrite source records; P&L snapshots are labeled PROVISIONAL.
 */
package com.mtms.closing;
