-- Runs once, only when the mysql-data volume is first initialized (the official MySQL image
-- executes every *.sql in /docker-entrypoint-initdb.d on first boot, after MYSQL_DATABASE/
-- MYSQL_USER/MYSQL_PASSWORD have already created platform_user and granted platform_app on it).
--
-- Each module-* owns its own schema (see platform-app's application.yml comment) - module-user
-- got platform_user via MYSQL_DATABASE/MYSQL_USER already; every module after it gets its schema
-- created and granted here, so a fresh checkout ends up with the same layout as an existing one.
CREATE DATABASE IF NOT EXISTS platform_post CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON platform_post.* TO 'platform_app'@'%';
FLUSH PRIVILEGES;
