package com.guyacevedo.factusimple.shared.exception;

public final class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(401, message, "UNAUTHORIZED");
    }

    public UnauthorizedException() {
        super(401, "No autorizado", "UNAUTHORIZED");
    }
}