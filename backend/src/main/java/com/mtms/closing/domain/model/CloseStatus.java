package com.mtms.closing.domain.model;

/**
 * P0-CLS-01 / BR-CLS-01 / TC-040
 * Lifecycle status of a branch's monthly business close.
 */
public enum CloseStatus {
    OPEN,
    CLOSING,
    CLOSED,
    REOPEN_REQUESTED,
    REOPENED,
    SUPERSEDED
}
