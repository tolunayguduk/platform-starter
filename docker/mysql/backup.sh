#!/bin/sh
# Periodic mysqldump of every module-* schema, run from a dedicated container (see the
# mysql-backup service in docker-compose.yml). Writes to a host bind-mount, not a named Docker
# volume - a `docker compose down -v` (or losing the Docker data root entirely, which has already
# happened once on this project) wipes named volumes but leaves the host filesystem untouched.
set -eu

BACKUP_DIR="/backups"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
INTERVAL_SECONDS="${BACKUP_INTERVAL_SECONDS:-21600}" # 6 hours
# Keep this list in sync with docker/mysql/init-schemas.sql - one schema per module-*.
DATABASES="platform_user platform_post"

mkdir -p "$BACKUP_DIR"

while true; do
  timestamp=$(date +%Y%m%d-%H%M%S)
  for db in $DATABASES; do
    echo "[backup] dumping $db at $timestamp"
    if mysqldump -h mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
        --single-transaction --routines --triggers --databases "$db" \
        | gzip > "$BACKUP_DIR/${db}-${timestamp}.sql.gz.tmp"; then
      mv "$BACKUP_DIR/${db}-${timestamp}.sql.gz.tmp" "$BACKUP_DIR/${db}-${timestamp}.sql.gz"
    else
      echo "[backup] WARNING: dump of $db failed, discarding partial file" >&2
      rm -f "$BACKUP_DIR/${db}-${timestamp}.sql.gz.tmp"
    fi
  done
  find "$BACKUP_DIR" -name '*.sql.gz' -mtime "+${RETENTION_DAYS}" -delete
  echo "[backup] cycle done, sleeping ${INTERVAL_SECONDS}s"
  sleep "$INTERVAL_SECONDS"
done
