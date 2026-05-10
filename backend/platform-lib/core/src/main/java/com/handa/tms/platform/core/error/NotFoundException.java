package com.handa.tms.platform.core.error;

public class NotFoundException extends DomainException {
    public NotFoundException(String entity, Object id) {
        super("not_found", 404, entity + " not found: " + id);
    }
    public NotFoundException(String message) {
        super("not_found", 404, message);
    }
}
