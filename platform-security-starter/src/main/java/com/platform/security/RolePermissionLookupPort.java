package com.platform.security;

import java.util.Collection;
import java.util.Set;

/**
 * Implemented by module-user (or whichever module owns the role/permission tables).
 * This starter never talks to the database directly - it only depends on this contract,
 * which keeps platform-security decoupled from the actual schema and makes it possible to
 * swap the implementation (e.g. to a dedicated authorization microservice) later without
 * touching any @PreAuthorize annotation anywhere in the codebase.
 */
public interface RolePermissionLookupPort {

    /**
     * @param roleNames role names as they appear in the JWT "roles"/"realm_access.roles" claim
     * @return the set of permission keys (e.g. "report:list", "payment:approve") granted to those roles
     */
    Set<String> resolvePermissions(Collection<String> roleNames);
}
