#!/usr/bin/env bash
set -e

# ==============================================================================
# HotelOS — P1.0 PostgreSQL Database & Schema Provisioning (HLD Aligned)
#
# Target Architecture:
# - Single logical database: hotelos
# - Schema-per-bounded-context:
#     * reception       (owner: reception_user)
#     * room_service    (owner: room_service_user)
#     * housekeeping    (owner: housekeeping_user)
#     * maintenance     (owner: maintenance_user)
#     * dashboard_read  (owner: dashboard_user)
#
# Security & Isolation:
# - Strict identifier validation: ^[a-zA-Z0-9_]+$
# - Safe psql parameterization: :'password' literal escaping & quote_ident / :"name" identifier quoting
# - Passwords with arbitrary special characters safely supported without injection
# - Revoke unsafe CREATE on public schema from PUBLIC
# - Zero cross-schema privileges granted
# - Zero business tables or application DDL (Flyway owns business DDL)
# ==============================================================================

DB_TARGET="${POSTGRES_DB:-hotelos}"
echo "Provisioning HotelOS persistence foundation for database: ${DB_TARGET}..."

# Safe identifier validator: alphanumeric + underscore only
validate_identifier() {
    local identifier="$1"
    if [[ ! "$identifier" =~ ^[a-zA-Z0-9_]+$ ]]; then
        echo "ERROR: Invalid SQL identifier '$identifier'. Must contain only alphanumeric characters and underscores." >&2
        exit 1
    fi
}

validate_identifier "$POSTGRES_USER"
validate_identifier "$DB_TARGET"

# 1. Lock down public schema (PostgreSQL 16 security standard: prevent arbitrary object creation)
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DB_TARGET" <<- 'EOF'
    REVOKE CREATE ON SCHEMA public FROM PUBLIC;
EOF

provision_bounded_context() {
    local schema_name="$1"
    local user_name="$2"
    local user_password="$3"

    validate_identifier "$schema_name"
    validate_identifier "$user_name"

    echo "Provisioning bounded context: schema='${schema_name}', user='${user_name}'..."

    # Pass parameters safely using psql variables
    # -v user_name: identifier
    # -v user_password: literal (psql :'user_password' escapes quotes/metacharacters safely)
    # -v schema_name: identifier
    # -v db_name: identifier
    psql -v ON_ERROR_STOP=1 \
         --username "$POSTGRES_USER" \
         --dbname "$DB_TARGET" \
         -v user_name="$user_name" \
         -v user_password="$user_password" \
         -v schema_name="$schema_name" \
         -v db_name="$DB_TARGET" <<- 'EOF'
        -- 1. Create role if not exists with encrypted password (safe literal escaping)
        SELECT 'CREATE ROLE ' || quote_ident(:'user_name') || ' WITH LOGIN ENCRYPTED PASSWORD ' || quote_literal(:'user_password')
        WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'user_name') \gexec

        -- 2. Update password if role already exists
        SELECT 'ALTER ROLE ' || quote_ident(:'user_name') || ' WITH PASSWORD ' || quote_literal(:'user_password')
        WHERE EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'user_name') \gexec

        -- 3. Grant database connection privilege
        GRANT CONNECT ON DATABASE :"db_name" TO :"user_name";

        -- 4. Create isolated schema owned by the service user
        CREATE SCHEMA IF NOT EXISTS :"schema_name" AUTHORIZATION :"user_name";

        -- 5. Enforce schema ownership and privileges
        ALTER SCHEMA :"schema_name" OWNER TO :"user_name";
        GRANT ALL PRIVILEGES ON SCHEMA :"schema_name" TO :"user_name";

        -- 6. Ensure no access from PUBLIC or foreign users
        REVOKE ALL ON SCHEMA :"schema_name" FROM PUBLIC;
EOF

    echo "Provisioned successfully: schema='${schema_name}', owner='${user_name}'"
}

# 2. Provision bounded contexts according to HLD
provision_bounded_context \
    "${RECEPTION_SCHEMA:-reception}" \
    "${RECEPTION_DB_USER:-reception_user}" \
    "${RECEPTION_DB_PASSWORD:-reception_dev_pass}"

provision_bounded_context \
    "${ROOM_SERVICE_SCHEMA:-room_service}" \
    "${ROOM_SERVICE_DB_USER:-room_service_user}" \
    "${ROOM_SERVICE_DB_PASSWORD:-room_service_dev_pass}"

provision_bounded_context \
    "${HOUSEKEEPING_SCHEMA:-housekeeping}" \
    "${HOUSEKEEPING_DB_USER:-housekeeping_user}" \
    "${HOUSEKEEPING_DB_PASSWORD:-housekeeping_dev_pass}"

provision_bounded_context \
    "${MAINTENANCE_SCHEMA:-maintenance}" \
    "${MAINTENANCE_DB_USER:-maintenance_user}" \
    "${MAINTENANCE_DB_PASSWORD:-maintenance_dev_pass}"

provision_bounded_context \
    "${DASHBOARD_SCHEMA:-dashboard_read}" \
    "${DASHBOARD_DB_USER:-dashboard_user}" \
    "${DASHBOARD_DB_PASSWORD:-dashboard_dev_pass}"

echo "All HotelOS bounded context schemas provisioned and isolated successfully in database '${DB_TARGET}'."
