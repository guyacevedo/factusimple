package com.guyacevedo.factusimple.shared.exception;

public final class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(403, message, "FORBIDDEN");
    }

    public ForbiddenException(String message, String errorCode) {
        super(403, message, errorCode);
    }
}
