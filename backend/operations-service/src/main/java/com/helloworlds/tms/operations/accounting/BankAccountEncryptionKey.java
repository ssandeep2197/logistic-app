package com.helloworlds.tms.operations.accounting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds the symmetric key used by pgcrypto's {@code pgp_sym_encrypt} /
 * {@code pgp_sym_decrypt} for {@link BankAccount} sensitive columns.
 * <p>
 * Sourced from the {@code BANK_ENCRYPTION_KEY} env var, which the K8s
 * deployment wires up from the {@code tms-accounting-secret} Secret.  The
 * key never lives in Postgres (avoiding the "encrypted at rest, key beside
 * the data" anti-pattern) and never goes to disk in this JVM either.
 * <p>
 * If the key is missing the service refuses to start — that's a hard fail
 * rather than silent fallback to a dev default, because a tenant's bank
 * data would otherwise be readable by anyone with DB access.
 */
@Component
public final class BankAccountEncryptionKey {

    private final String key;

    /** Placeholder used in application.yml for local dev. Refused at startup. */
    static final String DEV_PLACEHOLDER = "dev-only-not-for-prod-bank-key";

    public BankAccountEncryptionKey(@Value("${tms.bank.encryption-key:}") String key,
                                    @Value("${spring.profiles.active:default}") String profile) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "tms.bank.encryption-key (env BANK_ENCRYPTION_KEY) is not set; " +
                    "refusing to start to avoid writing bank rows with a default key");
        }
        if (DEV_PLACEHOLDER.equals(key) && profile.contains("prod")) {
            throw new IllegalStateException(
                    "tms.bank.encryption-key is still the dev placeholder under the prod " +
                    "profile; set BANK_ENCRYPTION_KEY to a real 32+ byte secret");
        }
        this.key = key;
    }

    /** Pass-through to native pgcrypto query parameters; never log this value. */
    public String key() {
        return key;
    }
}
