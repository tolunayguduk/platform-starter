package com.platform.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Binds:
 *
 * platform:
 *   cache:
 *     default-ttl: 120s
 *     ttls:
 *       role-permissions: 300s
 *       user-profile: 600s
 *
 * Adding a new cache name is a YAML change only - no code/redeploy needed to tune TTLs.
 */
@ConfigurationProperties(prefix = "platform.cache")
public class CacheProperties {

    private Duration defaultTtl = Duration.ofMinutes(2);
    private Map<String, Duration> ttls = new HashMap<>();

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getTtls() {
        return ttls;
    }

    public void setTtls(Map<String, Duration> ttls) {
        this.ttls = ttls;
    }
}
