package com.platform.user.admin;

import com.platform.error.BusinessException;
import com.platform.security.KeycloakAdminClient;
import com.platform.user.identity.UserCore;
import com.platform.user.identity.UserCoreRepository;
import com.platform.user.profile.UserProfile;
import com.platform.user.profile.UserProfileRepository;
import org.springframework.stereotype.Service;

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

/**
 * Backs the admin panel: the user list is built from the local mirror (fast, one query) enriched
 * with roles read from Keycloak (the source of truth for role assignment - see UserCore's javadoc).
 * Editing a user's roles only ever calls Keycloak; nothing about role membership is stored locally.
 */
@Service
public class AdminUserService {

    /** The only roles this panel knows how to display/edit - AUDITOR exists in the realm but has
     * no permission-matrix wiring yet, so it's deliberately left out of the editable set. */
    private static final List<String> MANAGED_ROLES = List.of("ADMIN", "MANAGER", "USER");

    private final UserCoreRepository userCoreRepository;
    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    public AdminUserService(UserCoreRepository userCoreRepository,
                             UserProfileRepository userProfileRepository,
                             KeycloakAdminClient keycloakAdminClient) {
        this.userCoreRepository = userCoreRepository;
        this.userProfileRepository = userProfileRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public List<AdminUserView> listUsers() {
        Map<String, Set<String>> rolesByKeycloakUserId = new HashMap<>();
        for (String role : MANAGED_ROLES) {
            for (String keycloakUserId : keycloakAdminClient.getUserIdsWithRole(role)) {
                rolesByKeycloakUserId.computeIfAbsent(keycloakUserId, k -> new TreeSet<>()).add(role);
            }
        }

        return userCoreRepository.findAll().stream()
                .map(core -> new AdminUserView(
                        core.getId(),
                        core.getUsername(),
                        core.getEmail(),
                        userProfileRepository.findById(core.getId()).map(UserProfile::getFullName).orElse(null),
                        core.getStatus(),
                        core.getCreatedAt(),
                        rolesByKeycloakUserId.getOrDefault(core.getKeycloakUserId(), Set.of())))
                .sorted(Comparator.comparing(AdminUserView::createdAt).reversed())
                .toList();
    }

    public void updateUserRoles(String userCoreId, Set<String> newRoles, String currentAdminKeycloakUserId) {
        for (String role : newRoles) {
            if (!MANAGED_ROLES.contains(role)) {
                throw new BusinessException("USER-4001", "error.admin.unknown_role", "Unknown role: " + role);
            }
        }

        UserCore core = userCoreRepository.findById(userCoreId)
                .orElseThrow(() -> new BusinessException("USER-4040", "error.admin.user_not_found",
                        "No such user: " + userCoreId));
        String keycloakUserId = core.getKeycloakUserId();

        // An admin editing their own row must not be able to strand the realm without one -
        // Keycloak has no "last admin" safeguard of its own.
        if (keycloakUserId.equals(currentAdminKeycloakUserId) && !newRoles.contains("ADMIN")) {
            throw new BusinessException("USER-4002", "error.admin.self_lockout",
                    "Cannot remove your own ADMIN role");
        }

        Set<String> currentRoles = MANAGED_ROLES.stream()
                .filter(role -> keycloakAdminClient.getUserIdsWithRole(role).contains(keycloakUserId))
                .collect(Collectors.toSet());

        for (String role : newRoles) {
            if (!currentRoles.contains(role)) {
                keycloakAdminClient.assignRealmRole(keycloakUserId, role);
            }
        }
        for (String role : currentRoles) {
            if (!newRoles.contains(role)) {
                keycloakAdminClient.removeRealmRole(keycloakUserId, role);
            }
        }
    }

    public List<RegistrationStatsPoint> getRegistrationStats(StatsRange range) {
        List<UserCore> users = userCoreRepository.findAll();
        ZoneId zone = ZoneId.systemDefault();
        return switch (range) {
            case DAY -> bucketByHour(users, zone);
            case WEEK -> bucketByDay(users, 7, zone);
            case MONTH -> bucketByDay(users, 30, zone);
            case YEAR -> bucketByMonth(users, zone);
        };
    }

    private List<RegistrationStatsPoint> bucketByHour(List<UserCore> users, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime start = nowBucket.minusHours(23);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:00");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 24; i++) {
            counts.put(fmt.format(start.plusHours(i)), 0L);
        }
        for (UserCore user : users) {
            ZonedDateTime created = user.getCreatedAt().atZone(zone).truncatedTo(ChronoUnit.HOURS);
            if (created.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(created), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPoint> bucketByDay(List<UserCore> users, int days, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime start = nowBucket.minusDays(days - 1L);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            counts.put(fmt.format(start.plusDays(i)), 0L);
        }
        for (UserCore user : users) {
            ZonedDateTime created = user.getCreatedAt().atZone(zone).truncatedTo(ChronoUnit.DAYS);
            if (created.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(created), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPoint> bucketByMonth(List<UserCore> users, ZoneId zone) {
        ZonedDateTime nowBucket = ZonedDateTime.now(zone).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime start = nowBucket.minusMonths(11);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(fmt.format(start.plusMonths(i)), 0L);
        }
        for (UserCore user : users) {
            ZonedDateTime createdMonth = user.getCreatedAt().atZone(zone).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            if (createdMonth.isBefore(start)) {
                continue;
            }
            counts.merge(fmt.format(createdMonth), 1L, Long::sum);
        }
        return toPoints(counts);
    }

    private List<RegistrationStatsPoint> toPoints(LinkedHashMap<String, Long> counts) {
        return counts.entrySet().stream()
                .map(e -> new RegistrationStatsPoint(e.getKey(), e.getValue()))
                .toList();
    }
}