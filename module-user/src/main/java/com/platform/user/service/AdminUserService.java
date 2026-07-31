package com.platform.user.service;

import com.platform.user.constant.StatsRange;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.RegistrationStatsPointResult;
import com.platform.user.service.model.UpdateUserRolesCommand;

import java.util.List;

/**
 * Backs the admin panel: identity and registration timestamp come straight from Keycloak - there
 * is no local user mirror anymore. Role membership is likewise resolved and edited only against
 * Keycloak, and the set of roles the panel lets an admin assign is whatever the realm currently
 * defines, never a hardcoded list.
 */
public interface AdminUserService {

    List<AdminUserResult> listUsers();

    void updateUserRoles(UpdateUserRolesCommand command);

    List<RegistrationStatsPointResult> getRegistrationStats(StatsRange range);
}
