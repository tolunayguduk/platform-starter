package com.platform.user.service;

import com.platform.error.BusinessException;
import com.platform.security.integration.keycloak.KeycloakAdminClient;
import com.platform.security.integration.keycloak.model.AdminEvent;
import com.platform.security.integration.keycloak.model.KeycloakUserSummary;
import com.platform.user.constant.StatsRange;
import com.platform.user.entity.UserProfile;
import com.platform.user.repository.UserProfileRepository;
import com.platform.user.service.model.AdminUserAuditEventResult;
import com.platform.user.service.model.AdminUserResult;
import com.platform.user.service.model.RegistrationStatsPointResult;
import com.platform.user.service.model.UpdateUserIdentityCommand;
import com.platform.user.service.model.UpdateUserRolesCommand;
import org.springframework.stereotype.Service;

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

    public AdminUserServiceImpl(UserProfileRepository userProfileRepository, KeycloakAdminClient keycloakAdminClient) {
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @Override
    public List<AdminUserResult> listUsers() {
        List<String> managedRoles = keycloakAdminClient.listRealmRoles();

        Map<String, Set<String>> rolesByKeycloakUserId = new HashMap<>();
        for (String role : managedRoles) {
            for (String keycloakUserId : keycloakAdminClient.getUserIdsWithRole(role)) {
                rolesByKeycloakUserId.computeIfAbsent(keycloakUserId, k -> new TreeSet<>()).add(role);
            }
        }

        return keycloakAdminClient.listUsers().stream()
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
    public void updateUserRoles(UpdateUserRolesCommand command) {
        List<String> managedRoles = keycloakAdminClient.listRealmRoles();
        for (String role : command.roles()) {
            if (!managedRoles.contains(role)) {
                throw new BusinessException("USER-4001", "error.admin.unknown_role", "Unknown role: " + role);
            }
        }

        String keycloakUserId = command.keycloakUserId();
        // An admin editing their own row must not be able to strand the realm without one -
        // Keycloak has no "last admin" safeguard of its own.
        if (keycloakUserId.equals(command.currentAdminKeycloakUserId()) && !command.roles().contains("ADMIN")) {
            throw new BusinessException("USER-4002", "error.admin.self_lockout",
                    "Cannot remove your own ADMIN role");
        }

        Set<String> currentRoles = managedRoles.stream()
                .filter(role -> keycloakAdminClient.getUserIdsWithRole(role).contains(keycloakUserId))
                .collect(Collectors.toSet());

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
        // Same reasoning as the ADMIN-role self-lockout guard in updateUserRoles - an admin must
        // not be able to disable the only account they can currently act through.
        if (!enabled && keycloakUserId.equals(currentAdminKeycloakUserId)) {
            throw new BusinessException("USER-4003", "error.admin.self_disable", "Cannot disable your own account");
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
    public List<AdminUserAuditEventResult> getUserAuditEvents(String keycloakUserId) {
        List<AdminEvent> events = keycloakAdminClient.getUserAdminEvents(keycloakUserId);
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
