package com.helloworlds.tms.identity.auth;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Argon2id with conservative parameters for a TMS use case (latency budget on
 * login: ~150ms).  bcrypt would also be fine; argon2 is just better against
 * GPU attacks if the DB ever leaks.
 */
@Component
public class PasswordHasher {

    private final PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
