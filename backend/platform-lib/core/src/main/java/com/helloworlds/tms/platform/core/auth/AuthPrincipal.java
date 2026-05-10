package com.helloworlds.tms.platform.core.auth;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated caller for the current request.  Built from the JWT and
 * exposed to controllers via Spring's {@code @AuthenticationPrincipal}.
 * <p>
 * Stays a record on purpose — these are passed around hot paths and we want
 * structural equality + immutability without boilerplate.
 *
 * @param userId        Caller's user id within the tenant.
 * @param tenantId      Tenant the caller is operating in.
 * @param email         Email at the time the JWT was issued (for logging only).
 * @param permissions   Effective permissions string set, of the form
 *                      {@code "<resource>:<action>:<scope>"}, e.g.
 *                      {@code "load:read:own_branch"}.  Pre-flattened by
 *                      identity-service so the receiving service doesn't have
 *                      to call back to expand groups → roles → permissions.
 * @param branchIds     Branch scope, if the user is constrained to specific
 *                      branches.  Empty set means "no branch restriction".
 */
public record AuthPrincipal(
        UUID userId,
        UUID tenantId,
        String email,
        Set<String> permissions,
        Set<UUID> branchIds
) {
    public boolean has(String permission) {
        return permissions.contains(permission);
    }
}
