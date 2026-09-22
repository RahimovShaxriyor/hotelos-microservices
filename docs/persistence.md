# HotelOS — Persistence Architecture & Guidelines (P1 Foundation)

## 1. Overview & Engine Specification

- **Database Engine**: PostgreSQL 16 (`postgres:16-alpine` pinned in Docker Compose).
- **Physical Topology (Local Development)**: A single physical PostgreSQL container running in Docker Compose with named volume `hotelos-postgres-data`.
- **Logical Topology (HLD Aligned)**: Single logical database `hotelos` with **Schema-per-Bounded-Context** isolation.
- **Classification**: **Production-oriented local persistence foundation** (designed for local development and bounded-context separation; enterprise requirements such as TLS, secret management, automated backup/restore, and dual migration/runtime roles will be hardened for production deployment).

---

## 2. Bounded Context Ownership Matrix

| Bounded Context | Database Name | Schema Name | Dedicated Runtime DB User | Host Exposed Port | Context Scope |
|---|---|---|---|---|---|
| **Reception** | `hotelos` | `reception` | `reception_user` | 5434 (internal: 5432) | **PERSISTED** (P1.1): Rooms, guest stays, billing ledger |
| **Room Service** | `hotelos` | `room_service` | `room_service_user` | 5434 (internal: 5432) | NOT MIGRATED YET (P1.2): Orders, menu items, charges |
| **Housekeeping** | `hotelos` | `housekeeping` | `housekeeping_user` | 5434 (internal: 5432) | NOT MIGRATED YET (P1.3): Cleaning tasks, cleaners, queue |
| **Maintenance** | `hotelos` | `maintenance` | `maintenance_user` | 5434 (internal: 5432) | NOT MIGRATED YET (P1.4): Issues, technicians, priority queue |
| **Dashboard** | `hotelos` | `dashboard_read` | `dashboard_user` | 5434 (internal: 5432) | NOT MIGRATED YET: Read models & audit log |
| **Identity** | *Deferred to P2* | N/A | N/A | N/A | Auth & RBAC service |
| **Gateway** | **NO DATABASE** | None | None | N/A | Stateless routing & OpenAPI boundary |
| **hotelos-common** | **NO DATABASE** | None | None | N/A | Shared transport & event contracts |

### Strict Schema Isolation Rules
1. **Engine-Level Catalog Isolation**:
   - `REVOKE CREATE ON SCHEMA public FROM PUBLIC;` prevents unauthorized object creation in the public schema.
   - Each service user has `CONNECT` on `hotelos` and `USAGE, CREATE` strictly on its own schema.
   - Access to foreign schemas is denied at the PostgreSQL engine level (`has_schema_privilege` evaluates to `false` for all foreign schema pairs).
2. **No Cross-Schema Business SQL**: Cross-schema queries, joins, and foreign keys between schemas are strictly prohibited.
3. **Communication Boundary**: All cross-context interactions occur exclusively via asynchronous RabbitMQ `EventEnvelope` messages or synchronous HTTP REST calls through Gateway.

---

## 3. Flyway Migration Strategy & Current State

### Current Reality (P1.1 Reception Persistence)
- **Reception Service**: **ACTIVE**.
  - Runtime Flyway: `flyway-core` and `flyway-database-postgresql` (managed by Spring Boot 3.3.0 BOM).
  - Schema: `reception`.
  - Migration: `reception-service/src/main/resources/db/migration/V1__init_reception_schema.sql`.
  - Schema History: `reception.flyway_schema_history`.
  - Authoritative State: PostgreSQL 16 is the sole source of truth for Rooms, GuestStays, and RoomServiceCharges.
- **Other Services** (`room-service`, `housekeeping-service`, `maintenance-service`, `dashboard-service`):
  - Flyway: **NOT ACTIVE YET**.
  - Persistence: **NOT MIGRATED YET** (in-memory state preserved until P1.2..P1.4).
  - `flyway_schema_history`: Does not exist in foreign schemas.

### Migration Standards
- **Location**: `<service>/src/main/resources/db/migration/`
- **Naming Standard**: `V{version}__{description}.sql` (e.g. `V1__init_reception_schema.sql`, `V2__add_index.sql`).
- **Append-Only & Immutable**: Once applied, migrations must never be edited. Changes require a new incremented version.
- **Deterministic**: Migrations must contain deterministic DDL/DML and no dynamic environment branches.
- **Baseline**: Clean databases start from `V1`; `baselineOnMigrate=false`.
- **Clean Disabled**: `spring.flyway.clean-disabled=true` prevents accidental data drops.
- **Isolated History Table**: Each service maintains its own `flyway_schema_history` table in its dedicated schema (`spring.flyway.schemas=<service_schema>`).

---

## 4. DDL Ownership & Hibernate / JPA

- **Flyway Owns DDL**: All schema objects, tables, constraints, sequences, and indexes are created exclusively via Flyway migrations.
- **Hibernate Policy**: `spring.jpa.hibernate.ddl-auto=validate` (or `none`). Automatic schema mutation by Hibernate (`create`, `create-drop`, `update`) is strictly forbidden.

---

## 5. Column & Modeling Conventions for Future Migrations

1. **Timestamps**: Always use `TIMESTAMPTZ` (UTC Instant representation). Never use timezone-unaware `TIMESTAMP`.
2. **Currency**: Always use `NUMERIC(19,2)` (or domain decimal precision). Never use `FLOAT` or `DOUBLE`.
3. **Identifiers**: UUID and domain string identifiers (e.g. room number `"101"`, order ID `"ORD-..."`) are preserved as `VARCHAR` or native `UUID`.
4. **Enums & Status**: Stored as `VARCHAR` with optional `CHECK` constraints for schema evolvability.
5. **Indexes**: Added on-demand based on verified query patterns in P1.1+, never speculatively.

---

## 6. Environment Configuration & Network Addressing

### Host vs. Docker Container Addressing
- **From Host Machine**:
  - PostgreSQL Port: `localhost:5434` (default mapped via `${POSTGRES_PORT:-5434}` to avoid conflicts with other local databases on 5432).
  - JDBC URL: `jdbc:postgresql://localhost:5434/hotelos?currentSchema=<schema>`
- **Inside Docker Internal Network**:
  - PostgreSQL Host/Port: `postgres:5432`
  - JDBC URL: `jdbc:postgresql://postgres:5432/hotelos?currentSchema=<schema>`

### Environment Variables
All settings support environment variable overrides with local development defaults:
- `POSTGRES_USER` (default: `postgres`)
- `POSTGRES_PASSWORD` (default: `postgres_dev_password`, LOCAL DEV ONLY)
- `POSTGRES_DB` (default: `hotelos`)
- `POSTGRES_PORT` (default: `5434` on host)
- `<SERVICE>_SCHEMA` (e.g. `reception`, `room_service`, `housekeeping`, `maintenance`, `dashboard_read`)
- `<SERVICE>_DB_USER` (e.g. `reception_user`, `room_service_user`, etc.)
- `<SERVICE>_DB_PASSWORD` (e.g. `reception_dev_pass`, LOCAL DEV ONLY)
- `<SERVICE>_DB_URL`

Template file [.env.example](file:///Users/shaxriyorraximov/Desktop/HotelOS/.env.example) is tracked in git. Local `.env` is git-ignored.

---

## 7. Docker Lifecycle & Operations

### One-Time Initialization Lifecycle
> [!IMPORTANT]
> Scripts in `/docker-entrypoint-initdb.d/` run **ONLY** when PostgreSQL initializes an **EMPTY** data directory (`PGDATA`).
> Modifying `01-init-databases.sh` or environment variables does **NOT** alter an already-initialized Docker volume.

### Healthcheck Semantics
The container healthcheck:
```yaml
test: ["CMD-SHELL", "pg_isready -U postgres -d hotelos"]
```
Proves that the PostgreSQL engine is accepting client connections. It does **not** indicate Flyway migration status or application readiness.

### Starting Infrastructure
```bash
docker compose up -d postgres rabbitmq
```

### Targeted PostgreSQL Volume Reprovisioning
To safely reset and re-initialize only the PostgreSQL database without deleting unrelated project volumes (such as message broker state):
```bash
docker compose stop postgres
docker compose rm -f postgres
docker volume rm hotelos_hotelos-postgres-data
docker compose up -d postgres
```
*(Note: This deletes all data in the PostgreSQL volume. Used for local development resets).*

### Connecting via psql
```bash
# As service user (scoped to own schema):
docker exec -it hotelos-postgres psql -U reception_user -d hotelos

# As admin superuser:
docker exec -it hotelos-postgres psql -U postgres -d hotelos
```
