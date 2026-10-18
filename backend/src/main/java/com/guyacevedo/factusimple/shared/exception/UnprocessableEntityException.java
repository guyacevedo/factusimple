package com.guyacevedo.factusimple.shared.exception;

public final class UnprocessableEntityException extends ApiException {

    public UnprocessableEntityException(String message) {
        super(422, message, "UNPROCESSABLE_ENTITY");
    }

    public UnprocessableEntityException(String message, String errorCode) {
        super(422, message, errorCode);
    }
}
