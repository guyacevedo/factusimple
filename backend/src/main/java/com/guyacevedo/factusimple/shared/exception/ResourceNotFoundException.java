package com.guyacevedo.factusimple.shared.exception;

public final class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(404,
                String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue),
                "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(404, message, "RESOURCE_NOT_FOUND");
    }
}