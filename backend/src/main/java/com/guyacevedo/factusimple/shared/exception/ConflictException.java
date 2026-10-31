package com.guyacevedo.factusimple.shared.exception;

public final class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(409, message, "CONFLICT");
    }

    public ConflictException(String message, String errorCode) {
        super(409, message, errorCode);
    }
}
