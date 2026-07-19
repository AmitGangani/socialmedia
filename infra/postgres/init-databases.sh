#!/usr/bin/env bash
set -Eeuo pipefail

required_variables=(
    USER_DB_PASSWORD
    POST_DB_PASSWORD
    FOLLOW_DB_PASSWORD
    TIMELINE_DB_PASSWORD
    NOTIFICATION_DB_PASSWORD
)

for variable_name in "${required_variables[@]}"; do
    if [[ -z "${!variable_name:-}" ]]; then
        echo "Required database credential is not set: ${variable_name}" >&2
        exit 1
    fi
done

ensure_role() {
    local role_name="$1"
    local role_password="$2"

    psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        --set=role_name="$role_name" --set=role_password="$role_password" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'role_name', :'role_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'role_name')
\gexec
SELECT format(
    'ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION',
    :'role_name', :'role_password'
)
\gexec
SQL
}

ensure_database() {
    local database_name="$1"
    local role_name="$2"

    psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        --set=database_name="$database_name" --set=role_name="$role_name" <<'SQL'
SELECT format('CREATE DATABASE %I OWNER %I', :'database_name', :'role_name')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'database_name')
\gexec
SELECT format('ALTER DATABASE %I OWNER TO %I', :'database_name', :'role_name')
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'database_name')
\gexec
SELECT format('GRANT CONNECT, TEMPORARY ON DATABASE %I TO %I', :'database_name', :'role_name')
\gexec
SQL

    psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$database_name" \
        --set=role_name="$role_name" <<'SQL'
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SELECT format('GRANT USAGE, CREATE ON SCHEMA public TO %I', :'role_name')
\gexec
SQL
}

ensure_role user_service "$USER_DB_PASSWORD"
ensure_role post_service "$POST_DB_PASSWORD"
ensure_role follow_service "$FOLLOW_DB_PASSWORD"
ensure_role timeline_service "$TIMELINE_DB_PASSWORD"
ensure_role notification_service "$NOTIFICATION_DB_PASSWORD"

ensure_database user_db user_service
ensure_database post_db post_service
ensure_database follow_db follow_service
ensure_database timeline_db timeline_service
ensure_database notification_db notification_service
