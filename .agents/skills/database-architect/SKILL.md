---
name: database-architect
description: Design and implement production-grade database architectures, schemas, and indexing strategies. Use this skill when designing SQL/NoSQL schemas, implementing full-text search (FTS), choosing indexing types (B-Tree, GIN, BRIN), writing migrations, managing multi-tenant database designs, optimizing query performance, or setting up caching layers (Redis). Triggers on: "design a database schema", "SQLite FTS5", "PostgreSQL indexing", "database migrations", "multi-tenant design", "JSONB vs. relational", "optimize query", "database architect", "SQL schema", "data modeling", "database performance". Always use this skill when data structure and query efficiency matter.
---

# Database Architect Skill

Design robust, scalable, and high-performance data architectures. This skill focuses on production-grade patterns for SQLite (local/embedded) and PostgreSQL (hosted/distributed).

---

## Architecture Decision: Relationship vs. JSONB

Choose the right model before writing a single `CREATE TABLE` statement:

- **Relational**: Use when data is structured, has high consistency requirements, and joins are frequent.
- **JSONB (PostgreSQL)**: Use when data has many optional fields, high variability, or is primarily "payload" that doesn't need indexing on every sub-field.
- **SQLite JSON**: Use when you need portability and simple document-like storage inside an embedded database.

---

## SQLite Design Patterns

### 1. High-Performance FTS5 (Full-Text Search)

SQLite's FTS5 is world-class for local search. Always use a virtual table and triggers to sync.

```sql
-- Main Table
CREATE TABLE documents (
  id INTEGER PRIMARY KEY,
  title TEXT NOT NULL,
  content TEXT NOT NULL
);

-- FTS5 Virtual Table
CREATE VIRTUAL TABLE documents_fts USING fts5(
  title, 
  content, 
  content='documents', 
  content_rowid='id'
);

-- Sync Triggers (Automatic Indexing)
CREATE TRIGGER docs_ai AFTER INSERT ON documents BEGIN
  INSERT INTO documents_fts(rowid, title, content) VALUES (new.id, new.title, new.content);
END;

CREATE TRIGGER docs_ad AFTER DELETE ON documents BEGIN
  INSERT INTO documents_fts(documents_fts, rowid, title, content) VALUES ('delete', old.id, old.title, old.content);
END;

CREATE TRIGGER docs_au AFTER UPDATE ON documents BEGIN
  INSERT INTO documents_fts(documents_fts, rowid, title, content) VALUES ('delete', old.id, old.title, old.content);
  INSERT INTO documents_fts(rowid, title, content) VALUES (new.id, new.title, new.content);
END;
```

---

## PostgreSQL Indexing Strategies

### 1. Choose the Right Index Type

- **B-Tree**: Default. Use for `=`, `<`, `>`, `BETWEEN`, `IN`.
- **GIN (Generalized Inverted Index)**: Essential for `JSONB` and arrays.
- **BRIN (Block Range Index)**: For massive tables (TB+) with naturally sorted data (e.g., timestamps).
- **Partial Index**: Only index rows you actually query.

```sql
-- Partial GIN index for active JSONB metadata
CREATE INDEX idx_active_meta ON events USING GIN (metadata) 
WHERE status = 'active';

-- Trigam index for fuzzy string search
CREATE EXTENSION pg_trgm;
CREATE INDEX idx_users_name_trgm ON users USING gist (name gist_trgm_ops);
```

---

## Multi-Tenant Architecture

Choose your isolation level based on scale and security requirements:

1. **Schema Separation**: One schema per tenant. (High isolation, complex migrations)
2. **Table Separation**: `tenant_id` column on every table. (Simple, standard, requires Row-Level Security)
3. **Database Separation**: One DB per tenant. (Max isolation, high overhead)

### Row-Level Security (RLS) Pattern (Recommended for Postgres)

```sql
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON documents 
USING (tenant_id = current_setting('app.current_tenant_id')::uuid);
```

---

## Database Migration Workflow

1. **Pre-deployment**: Run scripts to create new tables/columns. (Safe, non-breaking)
2. **Deployment**: Update application code to use both old and new columns.
3. **Backfill**: Migrate data from old fields to new ones in chunks.
4. **Cleanup**: Drop the old columns/tables once verification is complete.

---

## Database Quality Checklist

- [ ] **Primary Keys**: Every table has a PK (preferably UUID or BigInt).
- [ ] **Foreign Keys**: Cascades and constraints are explicitly defined.
- [ ] **Indexes**: No index on columns with low cardinality (e.g., booleans).
- [ ] **Audit Trail**: `created_at` and `updated_at` columns on all mutable tables.
- [ ] **Query Review**: `EXPLAIN ANALYZE` used on all complex queries.
- [ ] **Naming**: Consistent snake_case for tables and columns.
- [ ] **Normal Form**: Data is at least in 3rd Normal Form (3NF) unless denormalized for performance.

---

## Caching Strategy (Redis)

Use Redis for:
- Session data (ephemeral)
- Rate limiting counters
- Heavy computation results
- Pub/Sub events

**Pattern — Cache-Aside**:
1. Check Redis for key.
2. If Miss: Query DB, Load into Redis with TTL (Time To Live).
3. If Hit: Return data.
4. On Write: Invalidate Cache (Delete key).
