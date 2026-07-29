CREATE TABLE role (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_role_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permission (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    `key`     VARCHAR(150) NOT NULL,
    ui_policy VARCHAR(30)  NOT NULL DEFAULT 'HIDE_IF_DENIED',
    CONSTRAINT uq_permission_key UNIQUE (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permission (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES role (id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permission (id),
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- seed a couple of example roles/permissions so the app boots with something usable
INSERT INTO role (name) VALUES ('ADMIN'), ('MANAGER'), ('USER'), ('AUDITOR');

INSERT INTO permission (`key`, ui_policy) VALUES
    ('report:list', 'HIDE_IF_DENIED'),
    ('report:approve', 'DISABLE_IF_DENIED'),
    ('billing:refund', 'HIDE_IF_DENIED');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p WHERE r.name = 'ADMIN';

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p WHERE r.name = 'MANAGER' AND p.`key` IN ('report:list', 'report:approve');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.id, p.id FROM role r, permission p WHERE r.name = 'USER' AND p.`key` = 'report:list';
