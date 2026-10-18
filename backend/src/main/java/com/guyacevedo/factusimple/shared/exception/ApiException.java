package com.guyacevedo.factusimple.shared.exception;

import lombok.Getter;

/**
 * Base of the application's own errors. Abstract: always throw one of the
 * concrete subclasses so the HTTP status is explicit.
 */
@Getter
public abstract sealed class ApiException extends RuntimeException
        permits BadRequestException, ConflictException, ForbiddenException,
        ResourceNotFoundException, TooManyRequestsException, UnauthorizedException,
        UnprocessableEntityException {

    private final int statusCode;
    private final String errorCode;

    protected ApiException(int statusCode, String message, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    protected ApiException(int statusCode, String message) {
        this(statusCode, message, "UNKNOWN_ERROR");
    }
}