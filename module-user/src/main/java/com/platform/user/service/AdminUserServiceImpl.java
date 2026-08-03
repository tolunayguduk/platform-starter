package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.AdminEvent;
import com.platform.security.integration.keycloak.model.KeycloakGroup;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.security.integration.keycloak.model.RealmRole;
import com.platform.user.constant.MembershipRequestStatus;
import com.platform.user.constant.RoleScope;
import com.platform.user.constant.StatsRange;
import com.platform.user.entity.OrganizationMembershipRequest;
import com.platform.user.entity.RoleState;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.OrganizationMembershipRequestRepository;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    /** Keycloak's admin-event log is fetched (and capped) realm-wide - scanning this many of the
     * most recent events before filtering to an organization keeps the feed from going sparse just
     * because platform-wide activity from other organizations happened to be more recent. */
    private static final int RECENT_ACTIVITY_SCAN_WINDOW = 200;

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final UiPermissionsService uiPermissionsService;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleStateRepository roleStateRepository;
    private final AdminAccessScopeService adminAccessScopeService;
    private final OrganizationMembershipRequestRepository organizationMembershipRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AdminUserServiceImpl(UserProfileRepository userProfileRepository, KeycloakAdminClient keycloakAdminClient,
                                 UiPermissionsService uiPermissionsService, RolePermissionRepository rolePermissionRepository,
                                 RoleStateRepository roleStateRepository, AdminAccessScopeService adminAccessScopeService,
                                 OrganizationMembershipRequestRepository organizationMembershipRequestRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.uiPermissionsService = uiPermissionsService;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleStateRepository = roleStateRepository;
        this.adminAccessScopeService = adminAccessScopeService;
        this.organizationMembershipRequestRepository = organizationMembershipRequestRepository;
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
            Set<String> visibleUserIds = adminAccessScopeService.resolveVisibleUserIds(callerScope);
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
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
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
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
        RoleState state = loadOrNewRoleState(roleName);
        state.setScope(scope);
        roleStateRepository.save(state);
    }

    @Override
    public void createRole(String roleName, String callerKeycloakUserId) {
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
        if (isBlank(roleName)) {
            throw new BusinessException("COMMON-4001", "error.profile.missing_fields", "Role name required");
        }
        keycloakAdminClient.createRealmRole(roleName);
    }

    @Override
    public void updateRoleDescription(String roleName, String description, String callerKeycloakUserId) {
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
        keycloakAdminClient.updateRealmRoleDescription(roleName, description);
    }

    @Override
    @Transactional
    public void deleteRole(String roleName, String callerKeycloakUserId) {
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
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
            // Symmetric with the assignment guard above - an org-scoped caller must never touch a
            // platform-scope role's membership for anyone, including stripping it off a target user
            // who happens to share an organization with them.
            Set<String> removedRoles = currentRoles.stream().filter(role -> !command.roles().contains(role)).collect(Collectors.toSet());
            Set<String> platformRolesBeingRemoved = roleStateRepository.findByRoleNameIn(removedRoles).stream()
                    .filter(rs -> rs.getScope() == RoleScope.PLATFORM)
                    .map(RoleState::getRoleName)
                    .collect(Collectors.toSet());
            if (!platformRolesBeingRemoved.isEmpty()) {
                throw new BusinessException("USER-4008", "error.admin.platform_role_assignment_denied",
                        "Cannot remove platform-scope role(s): " + platformRolesBeingRemoved);
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
        // PLATFORM-only, unlike role assignment or removal - a manager runs their organization's
        // membership, but locking someone out of the platform entirely is an account-level action
        // only ADMIN performs.
        adminAccessScopeService.requirePlatformScope(currentAdminKeycloakUserId);
        keycloakAdminClient.setUserEnabled(keycloakUserId, enabled);
    }

    @Override
    public void updateUserIdentity(UpdateUserIdentityCommand command) {
        adminAccessScopeService.requirePlatformScope(command.callerKeycloakUserId());
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
    public Map<String, String> getUserUiPermissions(String keycloakUserId, String callerKeycloakUserId) {
        AdminAccessScope callerScope = adminAccessScopeService.resolve(callerKeycloakUserId);
        if (!callerScope.platformScoped()) {
            assertSharesOrganization(callerScope, keycloakUserId);
        }
        Set<String> roles = adminAccessScopeService.resolveUserRoles(keycloakUserId, keycloakAdminClient.listRealmRoles());
        return uiPermissionsService.getUiPermissions(roles);
    }

    @Override
    public List<AdminUserAuditEventResult> getUserAuditEvents(String keycloakUserId, String callerKeycloakUserId) {
        adminAccessScopeService.requirePlatformScope(callerKeycloakUserId);
        List<AdminEvent> events = keycloakAdminClient.getUserAdminEvents(keycloakUserId);
        return events.stream()
                .map(e -> new AdminUserAuditEventResult(
                        Instant.ofEpochMilli(e.time()), e.operationType(), e.resourcePath(), e.representation()))
                .toList();
    }

    @Override
    public List<AdminUserAuditEventResult> getRecentActivity(int limit, String callerKeycloakUserId) {
        AdminAccessScope callerScope = adminAccessScopeService.resolve(callerKeycloakUserId);
        // Always scan the widest available window before limiting - if we asked Keycloak for just
        // `limit` events up front and then filtered down to the caller's organization, platform-wide
        // noise could crowd out older-but-still-relevant events before the org filter ever saw them.
        List<AdminEvent> events = keycloakAdminClient.getRecentAdminEvents(RECENT_ACTIVITY_SCAN_WINDOW);
        if (!callerScope.platformScoped()) {
            Set<String> visibleUserIds = adminAccessScopeService.resolveVisibleUserIds(callerScope);
            events = events.stream()
                    .filter(e -> e.resourcePath() != null
                            && visibleUserIds.stream().anyMatch(id -> e.resourcePath().startsWith("users/" + id)))
                    .toList();
        }
        return events.stream()
                .limit(limit)
                .map(e -> new AdminUserAuditEventResult(
                        Instant.ofEpochMilli(e.time()), e.operationType(), e.resourcePath(), e.representation()))
                .toList();
    }

    @Override
    public List<RegistrationStatsPointResult> getRegistrationStats(StatsRange range, String callerKeycloakUserId) {
        AdminAccessScope callerScope = adminAccessScopeService.resolve(callerKeycloakUserId);
        List<KeycloakUserSummary> allUsers = keycloakAdminClient.listUsers();

        // Each organization is effectively its own "platform" for its manager - "registrations"
        // for an ORGANIZATION-scope caller means users who joined THIS organization, not users who
        // created a Keycloak account somewhere in the realm (which is what a PLATFORM-scope caller
        // sees, and is the only timestamp Keycloak itself tracks). A user's account may well
        // predate when they actually joined this particular organization.
        List<Instant> timestamps = callerScope.platformScoped()
                ? allUsers.stream().map(u -> Instant.ofEpochMilli(u.createdTimestamp())).toList()
                : resolveOrganizationJoinTimestamps(callerScope, allUsers);

        ZoneId zone = ZoneId.systemDefault();
        return switch (range) {
            case DAY -> bucketByHour(timestamps, zone);
            case WEEK -> bucketByDay(timestamps, 7, zone);
            case MONTH -> bucketByDay(timestamps, 30, zone);
            case YEAR -> bucketByMonth(timestamps, zone);
        };
    }

    /** When each currently-visible user joined the caller's organization(s) - the resolution time
     * of their accepted INVITE or approved JOIN_REQUEST (see OrganizationMembershipRequest). Falls
     * back to the user's own Keycloak account creation time for members with no such row: the
     * organization's creator (added as its first member directly, never through a request) and any
     * membership granted outside the invite/join-request flow (e.g. by direct admin action). */
    private List<Instant> resolveOrganizationJoinTimestamps(AdminAccessScope callerScope, List<KeycloakUserSummary> allUsers) {
        Set<String> visibleUserIds = adminAccessScopeService.resolveVisibleUserIds(callerScope);
        Map<String, Instant> accountCreatedAt = allUsers.stream()
                .collect(Collectors.toMap(KeycloakUserSummary::id, u -> Instant.ofEpochMilli(u.createdTimestamp())));

        Map<String, Instant> joinedAtByUserId = new HashMap<>();
        for (OrganizationMembershipRequest request : organizationMembershipRequestRepository
                .findByOrganizationIdInAndStatus(callerScope.organizationGroupIds(), MembershipRequestStatus.APPROVED)) {
            if (request.getResolvedAt() == null) {
                continue;
            }
            joinedAtByUserId.merge(request.getKeycloakUserId(), request.getResolvedAt(),
                    (existing, candidate) -> candidate.isAfter(existing) ? candidate : existing);
        }

        return visibleUserIds.stream()
                .map(userId -> joinedAtByUserId.getOrDefault(userId, accountCreatedAt.get(userId)))
                .filter(Objects::nonNull)
                .toList();
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

    /** "DAY" means the current calendar day (local midnight through now), not a rolling 24-hour
     * window - otherwise the displayed total silently depends on what hour it currently is (e.g.
     * at 01:00 a rolling window would still be showing mostly yesterday's registrations). */
    private List<RegistrationStatsPointResult> bucketByHour(List<Instant> timestamps, ZoneId zone) {
        ZonedDateTime start = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:00");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            counts.put(fmt.format(start.plusHours(i)), 0L);
        }
        for (Instant timestamp : timestamps) {
            ZonedDateTime bucketTime = timestamp.atZone(zone).truncatedTo(ChronoUnit.HOURS);
            if (bucketTime.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(bucketTime), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPointResult> bucketByDay(List<Instant> timestamps, int days, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime start = nowBucket.minusDays(days - 1L);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            counts.put(fmt.format(start.plusDays(i)), 0L);
        }
        for (Instant timestamp : timestamps) {
            ZonedDateTime bucketTime = timestamp.atZone(zone).truncatedTo(ChronoUnit.DAYS);
            if (bucketTime.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(bucketTime), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPointResult> bucketByMonth(List<Instant> timestamps, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime start = nowBucket.minusMonths(11);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(fmt.format(start.plusMonths(i)), 0L);
        }
        for (Instant timestamp : timestamps) {
            ZonedDateTime bucketMonth = timestamp.atZone(zone).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            if (bucketMonth.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(bucketMonth), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPointResult> toPoints(LinkedHashMap<String, Long> counts) {
        return counts.entrySet().stream()
                .map(e -> new RegistrationStatsPointResult(e.getKey(), e.getValue()))
                .toList();
    }
}
