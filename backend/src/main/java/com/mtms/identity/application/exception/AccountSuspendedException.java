package com.mtms.identity.application.exception;

import com.mtms.common.error.AccessForbiddenException;

public class AccountSuspendedException extends AccessForbiddenException {
    public AccountSuspendedException(String message) {
        super(message);
    }
}
