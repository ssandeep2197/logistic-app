package com.handa.tms.identity.auth;

import com.handa.tms.identity.domain.AppUser;
import com.handa.tms.identity.domain.Permission;
import com.handa.tms.identity.domain.Role;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Flattens a user's effective permissions: groups → roles → permissions,
 * unioned with direct user→role assignments.
 * <p>
 * Run on login + token refresh.  Result is shoved into the JWT so other
 * services don't have to call back to identity for every authorization check.
 */
@Component
public class PermissionResolver {

    public Set<String> resolve(AppUser user) {
        Set<String> codes = new HashSet<>();

        // Direct roles
        for (Role r : user.getDirectRoles()) {
            for (Permission p : r.getPermissions()) codes.add(p.code());
        }
        // Roles via groups
        for (var g : user.getGroups()) {
            for (Role r : g.getRoles()) {
                for (Permission p : r.getPermissions()) codes.add(p.code());
            }
        }
        return codes;
    }
}
