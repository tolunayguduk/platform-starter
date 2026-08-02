package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.AdminEvent;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.security.integration.keycloak.model.KeycloakUserId;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.security.integration.keycloak.model.RealmRole;
import com.platform.user.constant.RoleScope;
import com.platform.user.constant.StatsRange;
import com.platform.user.entity.RoleState;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.RolePermissionRepository;
import com.platform.user.repository.RoleStateRepository;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.AdminAccessScope;
import com.platform.user.service.model.AdminRoleResult;
import com.platform.user.service.model.AdminUserAuditEventResult;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.RegistrationStatsPointResult;
import com.platform.user.service.model.UpdateUserIdentityCommand;
import com.platform.user.service.model.UpdateUserRolesCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final UiPermissionsService uiPermissionsService;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleStateRepository roleStateRepository;
    private final AdminAccessScopeService adminAccessScopeService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminUserServiceImpl(UserProfileRepository userProfileRepository, KeycloakAdminClient keycloakAdminClient,
                                 UiPermissionsService uiPermissionsService, RolePermissionRepository rolePermissionRepository,
                                 RoleStateRepository roleStateRepository, AdminAccessScopeService adminAccessScopeService,
                                 ApplicationEventPublisher eventPublisher) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.uiPermissionsService = uiPermissionsService;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleStateRepository = roleStateRepository;
        this.adminAccessScopeService = adminAccessScopeService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<AdminUserResult> listUsers(String callerKeycloakUserId) {
        AdminAccessScope callerScope = adminAccessScopeService.resolve(callerKeycloakUserId);
        List<String> managedRoles = keycloakAdminClient.listRealmRoles();

        Map<String, Set<String>> rolesByKeycloakUserId = new HashMap<>();
        for (String role : managedRoles) {
            for (String keycloakUserId : keycloakAdminClient.getUserIdsWithRole(role)) {
                rolesByKeycloakUserId.computeIfAbsent(keycloakUserId, k -> new TreeSet<>()).add(role);
            }
        }

        List<KeycloakUserSummary> users = keycloakAdminClient.listUsers();
        if (!callerScope.platformScoped()) {
            Set<String> visibleUserIds = callerScope.organizationGroupIds().stream()
                    .flatMap(groupId -> keycloakAdminClient.getGroupMembers(groupId).stream().map(KeycloakUserId::id))
                    .collect(Collectors.toSet());
            users = users.stream().filter(u -> visibleUserIds.contains(u.id())).toList();
        }

        return users.stream()
                .map(kcUser -> new AdminUserResult(
                        kcUser.id(),
                        kcUser.username(),
                        kcUser.email(),
                        userProfileRepository.findById(kcUser.id()).map(UserProfile::getFullName).orElse(null),
                        kcUser.enabled() ? "ACTIVE" : "DISABLED",
                        Instant.ofEpochMilli(kcUser.createdTimestamp()),
                        rolesByKeycloakUserId.getOrDefault(kcUser.id(), Set.of())))
                .sorted(Comparator.comparing(AdminUserResult::createdAt).reversed())
                .toList();
    }

    @Override
    public List<AdminRoleResult> listManagedRoles() {
        List<RealmRole> roles = keycloakAdminClient.listRealmRolesDetailed();
        Map<String, RoleState> statesByRoleName = roleStateRepository.findByRoleNameIn(roles.stream().map(RealmRole::name).toList())
                .stream().collect(Collectors.toMap(RoleState::getRoleName, rs -> rs));
        return roles.stream()
                .map(r -> {
                    RoleState state = statesByRoleName.get(r.name());
                    boolean enabled = state == null || state.isEnabled();
                    RoleScope scope = state == null ? RoleScope.NONE : state.getScope();
                    return new AdminRoleResult(r.name(), r.description(), enabled, scope);
                })
                .toList();
    }

    @Override
    public void updateRoleStatus(String roleName, boolean enabled, String callerKeycloakUserId) {
        requirePlatformScope(callerKeycloakUserId);
        if (!enabled) {
            assertNotLastEnabledPlatformRole(roleName);
        }
        RoleState state = loadOrNewRoleState(roleName);
        state.setEnabled(enabled);
        roleStateRepository.save(state);
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleName));
    }

    @Override
    public void updateRoleScope(String roleName, RoleScope scope, String callerKeycloakUserId) {
        requirePlatformScope(callerKeycloakUserId);
        RoleState state = loadOrNewRoleState(roleName);
        state.setScope(scope);
        roleStateRepository.save(state);
    }

    @Override
    public void createRole(String roleName, String callerKeycloakUserId) {
        requirePlatformScope(callerKeycloakUserId);
        if (isBlank(roleName)) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Role name required");
        }
        keycloakAdminClient.createRealmRole(roleName);
    }

    @Override
    public void updateRoleDescription(String roleName, String description, String callerKeycloakUserId) {
        requirePlatformScope(callerKeycloakUserId);
        keycloakAdminClient.updateRealmRoleDescription(roleName, description);
    }

    @Override
    @Transactional
    public void deleteRole(String roleName, String callerKeycloakUserId) {
        requirePlatformScope(callerKeycloakUserId);
        assertNotLastEnabledPlatformRole(roleName);
        keycloakAdminClient.deleteRealmRole(roleName);
        // Keycloak is the source of truth for the role itself, but role_permission is our own
        // table - clean up its now-orphaned rows rather than leave dead grants nothing can reach.
        rolePermissionRepository.deleteAll(rolePermissionRepository.findByRoleName(roleName));
        eventPublisher.publishEvent(new RolePermissionsChangedEvent(roleName));
    }

    @Override
    public void updateUserRoles(UpdateUserRolesCommand command) {
        List<String> managedRoles = keycloakAdminClient.listRealmRoles();
        for (String role : command.roles()) {
            if (!managedRoles.contains(role)) {
                throw new BusinessException("USER-4001", "error.admin.unknown_role", "Unknown role: " + role);
            }
        }

        String keycloakUserId = command.keycloakUserId();
        String callerKeycloakUserId = command.currentAdminKeycloakUserId();
        AdminAccessScope callerScope = adminAccessScopeService.resolve(callerKeycloakUserId);

        // An admin editing their own row must not be able to strand the realm without a
        // platform-scope admin - Keycloak has no "last admin" safeguard of its own. Scope-driven,
        // not name-driven: whichever role(s) happen to be PLATFORM-scoped today.
        if (keycloakUserId.equals(callerKeycloakUserId) && callerScope.platformScoped()) {
            boolean willRetainPlatformScope = roleStateRepository.findByRoleNameIn(command.roles()).stream()
                    .anyMatch(rs -> rs.isEnabled() && rs.getScope() == RoleScope.PLATFORM);
            if (!willRetainPlatformScope) {
                throw new BusinessException("USER-4002", "error.admin.self_lockout",
                        "Cannot remove your own platform-scope role");
            }
        }

        Set<String> currentRoles = adminAccessScopeService.resolveUserRoles(keycloakUserId, managedRoles);

        Set<String> newlyAssignedRoles = command.roles().stream().filter(role -> !currentRoles.contains(role)).collect(Collectors.toSet());
        Set<String> disabledAmongNewlyAssigned = roleStateRepository.findByRoleNameIn(newlyAssignedRoles).stream()
                .filter(rs -> !rs.isEnabled())
                .map(RoleState::getRoleName)
                .collect(Collectors.toSet());
        if (!disabledAmongNewlyAssigned.isEmpty()) {
            throw new BusinessException("USER-4005", "error.admin.role_disabled",
                    "Cannot assign disabled role(s): " + disabledAmongNewlyAssigned);
        }

        if (!callerScope.platformScoped()) {
            assertSharesOrganization(callerScope, keycloakUserId);
            Set<String> platformRolesBeingAssigned = roleStateRepository.findByRoleNameIn(newlyAssignedRoles).stream()
                    .filter(rs -> rs.getScope() == RoleScope.PLATFORM)
                    .map(RoleState::getRoleName)
                    .collect(Collectors.toSet());
            if (!platformRolesBeingAssigned.isEmpty()) {
                throw new BusinessException("USER-4008", "error.admin.platform_role_assignment_denied",
                        "Cannot assign platform-scope role(s): " + platformRolesBeingAssigned);
            }
        }

        for (String role : command.roles()) {
            if (!currentRoles.contains(role)) {
                keycloakAdminClient.assignRealmRole(keycloakUserId, role);
            }
        }
        for (String role : currentRoles) {
            if (!command.roles().contains(role)) {
                keycloakAdminClient.removeRealmRole(keycloakUserId, role);
            }
        }
    }

    @Override
    public void updateUserStatus(String keycloakUserId, boolean enabled, String currentAdminKeycloakUserId) {
        // Same reasoning as the self-lockout guard in updateUserRoles - an admin must not be able
        // to disable the only account they can currently act through.
        if (!enabled && keycloakUserId.equals(currentAdminKeycloakUserId)) {
            throw new BusinessException("USER-4003", "error.admin.self_disable", "Cannot disable your own account");
        }
        AdminAccessScope callerScope = adminAccessScopeService.resolve(currentAdminKeycloakUserId);
        if (!callerScope.platformScoped()) {
            assertSharesOrganization(callerScope, keycloakUserId);
        }
        keycloakAdminClient.setUserEnabled(keycloakUserId, enabled);
    }

    @Override
    public void updateUserIdentity(UpdateUserIdentityCommand command) {
        if (isBlank(command.username()) || isBlank(command.email())) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Required field missing");
        }
        if (!command.email().contains("@")) {
            throw new BusinessException("COMMON-4002", "error.profile.invalid_email", "Invalid email: " + command.email());
        }
        keycloakAdminClient.updateUserIdentity(command.keycloakUserId(), command.username(), command.email());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public Map<String, String> getUserUiPermissions(String keycloakUserId) {
        Set<String> roles = adminAccessScopeService.resolveUserRoles(keycloakUserId, keycloakAdminClient.listRealmRoles());
        return uiPermissionsService.getUiPermissions(roles);
    }

    @Override
    public List<AdminUserAuditEventResult> getUserAuditEvents(String keycloakUserId) {
        List<AdminEvent> events = keycloakAdminClient.getUserAdminEvents(keycloakUserId);
        return events.stream()
                .map(e -> new AdminUserAuditEventResult(
                        Instant.ofEpochMilli(e.time()), e.operationType(), e.resourcePath(), e.representation()))
                .toList();
    }

    @Override
    public List<AdminUserAuditEventResult> getRecentActivity(int limit) {
        List<AdminEvent> events = keycloakAdminClient.getRecentAdminEvents(limit);
        return events.stream()
                .map(e -> new AdminUserAuditEventResult(
                        Instant.ofEpochMilli(e.time()), e.operationType(), e.resourcePath(), e.representation()))
                .toList();
    }

    @Override
    public List<RegistrationStatsPointResult> getRegistrationStats(StatsRange range) {
        List<KeycloakUserSummary> users = keycloakAdminClient.listUsers();
        ZoneId zone = ZoneId.systemDefault();
        return switch (range) {
            case DAY -> bucketByHour(users, zone);
            case WEEK -> bucketByDay(users, 7, zone);
            case MONTH -> bucketByDay(users, 30, zone);
            case YEAR -> bucketByMonth(users, zone);
        };
    }

    /** Guards deleteRole/updateRoleStatus(enabled=false) - Keycloak has no "last admin" safeguard
     * of its own, and unlike updateUserRoles' self-lockout check, this protects the role itself
     * from disappearing entirely, regardless of who's doing the removing. */
    private void assertNotLastEnabledPlatformRole(String roleName) {
        RoleState current = roleStateRepository.findById(roleName).orElse(null);
        boolean currentIsEnabledPlatform = current != null && current.isEnabled() && current.getScope() == RoleScope.PLATFORM;
        if (!currentIsEnabledPlatform) {
            return; // not a platform role, or already disabled - nothing to protect
        }
        List<String> allRoles = keycloakAdminClient.listRealmRoles();
        boolean anotherPlatformRoleExists = roleStateRepository.findByRoleNameIn(allRoles).stream()
                .anyMatch(rs -> !rs.getRoleName().equals(roleName) && rs.isEnabled() && rs.getScope() == RoleScope.PLATFORM);
        if (!anotherPlatformRoleExists) {
            throw new BusinessException("USER-4004", "error.admin.last_platform_role",
                    "Cannot remove/disable the last platform-scope role");
        }
    }

    private void requirePlatformScope(String callerKeycloakUserId) {
        if (!adminAccessScopeService.resolve(callerKeycloakUserId).platformScoped()) {
            throw new BusinessException("USER-4006", "error.admin.platform_scope_required",
                    "Only a platform-scope admin can perform this action");
        }
    }

    private void assertSharesOrganization(AdminAccessScope callerScope, String targetKeycloakUserId) {
        Set<String> targetGroupIds = keycloakAdminClient.getUserGroups(targetKeycloakUserId).stream()
                .map(KeycloakGroup::id)
                .collect(Collectors.toSet());
        boolean sharesOrganization = targetGroupIds.stream().anyMatch(callerScope.organizationGroupIds()::contains);
        if (!sharesOrganization) {
            throw new BusinessException("USER-4007", "error.admin.outside_organization",
                    "Cannot manage a user outside your organization");
        }
    }

    private RoleState loadOrNewRoleState(String roleName) {
        return roleStateRepository.findById(roleName).orElseGet(() -> {
            RoleState s = new RoleState();
            s.setRoleName(roleName);
            return s;
        });
    }

    private List<RegistrationStatsPointResult> bucketByHour(List<KeycloakUserSummary> users, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime start = nowBucket.minusHours(23);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:00");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            counts.put(fmt.format(start.plusHours(i)), 0L);
        }
        for (KeycloakUserSummary user : users) {
            ZonedDateTime created = Instant.ofEpochMilli(user.createdTimestamp()).atZone(zone).truncatedTo(ChronoUnit.HOURS);
            if (created.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(created), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPointResult> bucketByDay(List<KeycloakUserSummary> users, int days, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime start = nowBucket.minusDays(days - 1L);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            counts.put(fmt.format(start.plusDays(i)), 0L);
        }
        for (KeycloakUserSummary user : users) {
            ZonedDateTime created = Instant.ofEpochMilli(user.createdTimestamp()).atZone(zone).truncatedTo(ChronoUnit.DAYS);
            if (created.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(created), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPointResult> bucketByMonth(List<KeycloakUserSummary> users, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime start = nowBucket.minusMonths(11);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(fmt.format(start.plusMonths(i)), 0L);
        }
        for (KeycloakUserSummary user : users) {
            ZonedDateTime createdMonth = Instant.ofEpochMilli(user.createdTimestamp())
                    .atZone(zone).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            if (createdMonth.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(createdMonth), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPointResult> toPoints(LinkedHashMap<String, Long> counts) {
        return counts.entrySet().stream()
                .map(e -> new RegistrationStatsPointResult(e.getKey(), e.getValue()))
                .toList();
    }
}
