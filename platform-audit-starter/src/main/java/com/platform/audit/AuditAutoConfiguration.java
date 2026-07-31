package com.platform.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * Envers itself needs no explicit @Bean wiring beyond being on the classpath and the
 * hibernate.integration.envers.enabled=true property (see application.yml) - this class
 * exists mainly to make PlatformRevisionEntity discoverable from the app module's
 * entity scan without every module needing to know its package.
 */
@AutoConfiguration
@EntityScan(basePackageClasses = PlatformRevisionEntity.class)
public class AuditAutoConfiguration {
}
