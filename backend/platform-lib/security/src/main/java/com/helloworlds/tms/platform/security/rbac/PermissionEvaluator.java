package com.helloworlds.tms.platform.security.rbac;

import com.helloworlds.tms.platform.core.auth.AuthPrincipal;
import com.helloworlds.tms.platform.core.error.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The single place every service asks "is the current user allowed to X?".
 * <p>
 * Permissions in the JWT are pre-flattened strings of the form
 * {@code resource:action:scope} where scope is one of:
 * <ul>
 *   <li>{@code all}        — any record in the tenant</li>
 *   <li>{@code own_branch} — records in one of the user's branches</li>
 *   <li>{@code own}        — records the user owns / is assigned to</li>
 * </ul>
 * <p>
 * For scoped checks, callers must pass the relevant ids — branchId on records,
 * ownerId on records — so we can match against the user's claims.  No DB
 * lookups happen here; everything we need is already in the JWT.
 */
@Component
public class PermissionEvaluator {

    public AuthPrincipal current() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(p instanceof AuthPrincipal ap)) {
            throw new IllegalStateException("No AuthPrincipal in SecurityContext");
        }
        return ap;
    }

    /** Throws {@link ForbiddenException} unless the caller has {@code resource:action:all}. */
    public void requireAll(String resource, String action) {
        require(resource + ":" + action + ":all");
    }

    /**
     * Allow if the caller has {@code resource:action:all}, OR they have
     * {@code resource:action:own_branch} and {@code branchId} is in their branch set.
     */
    public void requireBranch(String resource, String action, UUID branchId) {
        AuthPrincipal me = current();
        if (me.has(resource + ":" + action + ":all")) return;
        if (me.has(resource + ":" + action + ":own_branch")
                && branchId != null && me.branchIds().contains(branchId)) return;
        throw new ForbiddenException(resource + ":" + action);
    }

    /**
     * Allow if all/own_branch (per branch rules), or {@code own} and the caller
     * is the {@code ownerId}.
     */
    public void requireOwn(String resource, String action, UUID ownerId, UUID branchId) {
        AuthPrincipal me = current();
        if (me.has(resource + ":" + action + ":all")) return;
        if (me.has(resource + ":" + action + ":own_branch")
                && branchId != null && me.branchIds().contains(branchId)) return;
        if (me.has(resource + ":" + action + ":own")
                && ownerId != null && ownerId.equals(me.userId())) return;
        throw new ForbiddenException(resource + ":" + action);
    }

    private void require(String permission) {
        if (!current().has(permission)) {
            throw new ForbiddenException(permission);
        }
    }
}
