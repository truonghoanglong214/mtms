package com.mtms.identity.application.exception;

import com.mtms.common.error.UnauthorizedException;

public class AuthenticationFailedException extends UnauthorizedException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
