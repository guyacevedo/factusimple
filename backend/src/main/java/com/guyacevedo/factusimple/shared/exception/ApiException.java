package com.guyacevedo.factusimple.shared.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public sealed class ApiException extends RuntimeException
        permits BadRequestException, ConflictException, ForbiddenException,
                ResourceNotFoundException, UnauthorizedException, UnprocessableEntityException {

    private final int statusCode;
    private final String errorCode;

    public ApiException(int statusCode, String message, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApiException(int statusCode, String message) {
        this(statusCode, message, "UNKNOWN_ERROR");
    }
}