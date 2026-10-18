package com.guyacevedo.factusimple.shared.exception;

public final class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(400, message, "BAD_REQUEST");
    }

    public BadRequestException(String message, String errorCode) {
        super(400, message, errorCode);
    }
}
