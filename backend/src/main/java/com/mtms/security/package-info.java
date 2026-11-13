/**
 * Bounded Context: Security.
 * Owns evidence file metadata/access, audit events, and idempotency records.
 * Must NOT own domain state transitions.
 */
package com.mtms.security;
