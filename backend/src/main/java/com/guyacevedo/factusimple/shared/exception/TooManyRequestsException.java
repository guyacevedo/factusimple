package com.guyacevedo.factusimple.shared.exception;

public final class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(429, message, "TOO_MANY_REQUESTS");
    }

    public TooManyRequestsException(String message, String errorCode) {
        super(429, message, errorCode);
    }
}