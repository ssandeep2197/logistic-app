package com.handa.tms.platform.core.error;

public class ForbiddenException extends DomainException {
    public ForbiddenException(String permission) {
        super("forbidden", 403, "Missing required permission: " + permission);
    }
}
