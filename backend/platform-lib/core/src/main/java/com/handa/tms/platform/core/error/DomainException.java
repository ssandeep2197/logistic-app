package com.handa.tms.platform.core.error;

/**
 * Superclass for all expected business-rule failures that should map to a 4xx
 * response.  Subclass it for specific cases ({@link NotFoundException},
 * {@link ConflictException}); throw a plain {@code DomainException} only when
 * none fits.
 * <p>
 * The exception handler in {@code platform-lib-web} renders these as RFC 7807
 * {@code application/problem+json}; their {@link #getCode()} becomes the
 * {@code "code"} field clients can switch on.
 */
public class DomainException extends RuntimeException {

    private final String code;
    private final int status;

    public DomainException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public DomainException(String code, int status, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public int getStatus() { return status; }
}
