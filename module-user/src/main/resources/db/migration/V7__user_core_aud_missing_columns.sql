-- V4 created user_core_aud without last_synced_at/created_at, even though UserCore maps both
-- (@Column(name = "last_synced_at"), @Column(name = "created_at")) and they exist on user_core (V1).
-- Hibernate's schema validation checks every @Audited column has a matching _aud column, so add them here.

ALTER TABLE user_core_aud
    ADD COLUMN last_synced_at DATETIME(6),
    ADD COLUMN created_at DATETIME(6);