package com.helloworlds.tms.platform.core.error;

public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super("conflict", 409, message);
    }
}
