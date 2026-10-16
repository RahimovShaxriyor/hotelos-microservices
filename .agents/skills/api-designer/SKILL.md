---
name: api-designer
description: Design and build production-quality REST APIs with consistent patterns, proper error handling, authentication, rate limiting, and OpenAPI documentation. Use this skill when designing API endpoints, structuring API responses, writing OpenAPI/Swagger specs, designing authentication schemes, planning versioning strategies, building Express/Fastify/Hono APIs, or reviewing existing API design. Triggers on: "design an API", "REST API", "API endpoints", "OpenAPI", "Swagger", "API authentication", "rate limiting", "API versioning", "endpoint structure", "API response format", "status codes", "Express API", "Fastify", "Hono". Always use this skill when API design consistency and developer experience are important.
---

# API Designer Skill

Build REST APIs with consistent patterns, excellent developer experience, and production-ready infrastructure. This skill covers endpoint design, response schemas, auth, errors, rate limiting, and documentation.

## API Design Principles

A well-designed API satisfies three audiences:
1. **The developer integrating it** — they need consistency and predictability
2. **The agent consuming it** — they need structured, typed, parseable responses
3. **The operator running it** — they need observability and safety controls

---

## Endpoint Design

### URL Structure

```
/resources                  — Collection (plural noun)
/resources/:id              — Single resource
/resources/:id/sub-resources — Nested resource

Examples:
GET  /skills                 — List skills
POST /skills                 — Create skill (or trigger creation)
GET  /skills/linear.app      — Get one skill
DELETE /skills/cache/linear.app — Clear cache for one skill

Avoid:
/getSkill          — No verbs in resource paths
/skill             — Use plural
/skills/get        — HTTP method encodes the action
```

### HTTP Methods

```
GET     — Read, safe, idempotent, cacheable
POST    — Create, trigger action, not idempotent
PUT     — Full replace, idempotent
PATCH   — Partial update, not idempotent
DELETE  — Remove, idempotent
```

### Query Parameters

```
Filtering:  ?niche=fintech&tags=dark-mode,minimalist
Sorting:    ?sort=analyzed_at&order=desc
Pagination: ?limit=20&offset=40  (or ?page=3&per_page=20)
Fields:     ?fields=domain,niche,tags  (sparse fieldsets)
Search:     ?q=dark+minimal+developer
```

---

## Response Schema Design

### Standard Response Envelope

Every API should use a consistent envelope. Choose one pattern and stick to it:

**Option A: Flat (recommended for simple APIs)**
```json
// Success
{ "domain": "stripe.com", "niche": "Fintech", ... }

// Success collection
{ "count": 50, "items": [...], "next": "/skills?offset=50" }

// Error
{ "error": "rate_limit_exceeded", "message": "10 free analyses used. Resets 2026-04-01.", "reset_at": "2026-04-01T00:00:00Z" }
```

**Option B: Wrapped (good for APIs with metadata)**
```json
// Success
{
  "data": { "domain": "stripe.com", ... },
  "meta": { "analyzed_at": "2026-03-14T10:00:00Z", "cache": "HIT" }
}

// Error
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "...",
    "details": { "reset_at": "..." }
  }
}
```

### Pagination

```json
{
  "count": 50,
  "total": 234,
  "limit": 20,
  "offset": 40,
  "has_more": true,
  "items": [...],
  "links": {
    "prev": "/skills?limit=20&offset=20",
    "next": "/skills?limit=20&offset=60"
  }
}
```

---

## Error Design

### Error Response Schema

```json
{
  "error": "snake_case_error_code",
  "message": "Human-readable explanation in plain language.",
  "details": {},
  "request_id": "req_abc123"
}
```

### Error Code Conventions

```
invalid_request      — Bad input (400)
unauthorized         — No valid auth (401)
forbidden            — Auth valid, access denied (403)
not_found            — Resource doesn't exist (404)
method_not_allowed   — Wrong HTTP method (405)
rate_limit_exceeded  — Quota hit (429)
validation_error     — Schema validation failed (422)
server_error         — Internal error (500)
service_unavailable  — Dependency down (503)
```

### Status Code Rules

```
200 OK              — Successful GET, PATCH
201 Created         — Successful POST (resource created)
202 Accepted        — Async operation started
204 No Content      — Successful DELETE
400 Bad Request     — Client error, bad input
401 Unauthorized    — Missing or invalid auth
403 Forbidden       — Auth valid, not allowed
404 Not Found       — Resource doesn't exist
409 Conflict        — State conflict (already exists)
422 Unprocessable   — Valid format, invalid semantics
429 Too Many Requests — Rate limit
500 Internal Error  — Server fault
503 Unavailable     — Dependency down, retry later
```

---

## Authentication Patterns

### API Key (recommended for developer tools)

```
Header: x-api-key: uisk_a8f3k29xm7q1r5t6y2w4e0n1b3v8c9
```

Express middleware:
```javascript
export function apiKeyAuth(req, res, next) {
  const key = req.headers['x-api-key'];
  if (!key) {
    return res.status(401).json({ error: 'unauthorized', message: 'Missing x-api-key header.' });
  }
  const tier = validateKey(key); // returns 'free' | 'starter' | 'pro' | 'admin' | null
  if (!tier) {
    return res.status(401).json({ error: 'unauthorized', message: 'Invalid API key.' });
  }
  req.apiTier = tier;
  req.apiKey = key;
  next();
}
```

### JWT Bearer (recommended for user-authenticated APIs)

```
Header: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### OAuth 2.0 (for third-party integrations)

Use when external products need to access user data on their behalf. Out of scope for V1 developer tools — use API keys instead.

---

## Rate Limiting

### Rate Limit Headers (always include)

```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 2026-04-01T00:00:00Z
X-RateLimit-Window: monthly
```

### SQLite-Based Rate Limiter

```javascript
// src/middleware.js
export function rateLimiter(req, res, next) {
  if (req.apiTier === 'pro' || req.apiTier === 'admin') return next();

  const limit = req.apiTier === 'starter' ? 50 : 10;
  const month = new Date().toISOString().slice(0, 7); // '2026-03'

  const usage = db.prepare(
    'SELECT count FROM rate_limits WHERE api_key = ? AND month = ?'
  ).get(req.apiKey, month);

  const current = usage?.count ?? 0;

  res.setHeader('X-RateLimit-Limit', limit);
  res.setHeader('X-RateLimit-Remaining', Math.max(0, limit - current));
  res.setHeader('X-RateLimit-Reset', new Date(new Date().getFullYear(), new Date().getMonth() + 1, 1).toISOString());

  if (current >= limit) {
    return res.status(429).json({
      error: 'rate_limit_exceeded',
      message: `You have used all ${limit} requests this month.`,
      reset_at: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 1).toISOString(),
      upgrade_url: 'https://api.example.com/upgrade',
    });
  }

  // Increment counter
  db.prepare(
    'INSERT INTO rate_limits (api_key, month, count) VALUES (?, ?, 1) ON CONFLICT(api_key, month) DO UPDATE SET count = count + 1'
  ).run(req.apiKey, month);

  next();
}
```

---

## Express API Structure

### src/server.js — Complete skeleton

```javascript
import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import morgan from 'morgan';
import { apiKeyAuth } from './middleware.js';
import { skillsRouter } from './routes/skills.js';
import { analyzeRouter } from './routes/analyze.js';

const app = express();

// Security
app.use(helmet());
app.use(cors({ origin: process.env.CORS_ORIGIN || false }));

// Parsing
app.use(express.json({ limit: '1mb' }));

// Logging (stderr only — never stdout)
app.use(morgan('short', { stream: process.stderr }));

// Auth (before all protected routes)
app.use('/skills', apiKeyAuth);
app.use('/analyze', apiKeyAuth);

// Routes
app.get('/health', (req, res) => res.json({ status: 'ok', version: process.env.npm_package_version }));
app.use('/skills', skillsRouter);
app.use('/analyze', analyzeRouter);

// 404 handler
app.use((req, res) => res.status(404).json({ error: 'not_found', message: `${req.method} ${req.path} not found` }));

// Error handler
app.use((err, req, res, next) => {
  console.error(err);
  const status = err.status || err.statusCode || 500;
  res.status(status).json({
    error: err.code || 'server_error',
    message: status >= 500 ? 'Internal server error' : err.message,
    request_id: req.id,
  });
});

export default app;
```

### Route pattern

```javascript
// src/routes/skills.js
import { Router } from 'express';
import { db } from '../db.js';

export const skillsRouter = Router();

skillsRouter.get('/', async (req, res, next) => {
  try {
    const { niche, tags, limit = 50, offset = 0 } = req.query;
    // Build query...
    const skills = db.prepare('SELECT ...').all();
    res.json({ count: skills.length, items: skills });
  } catch (err) {
    next(err); // passes to error handler
  }
});

skillsRouter.get('/:domain', async (req, res, next) => {
  try {
    const skill = db.prepare('SELECT * FROM analyses WHERE domain = ?').get(req.params.domain);
    if (!skill) {
      return res.status(404).json({
        error: 'not_found',
        message: `No skill found for ${req.params.domain}`,
        suggestion: 'Use POST /analyze to generate a skill for this domain.'
      });
    }
    res.json(JSON.parse(skill.skill_json));
  } catch (err) {
    next(err);
  }
});
```

---

## OpenAPI 3.1 Spec Template

```yaml
openapi: 3.1.0
info:
  title: UIskill API
  version: 1.0.0
  description: UI intelligence platform. Analyzes websites and returns structured design skills.

servers:
  - url: https://api.uiskill.dev
    description: Production
  - url: http://localhost:3000
    description: Local development

security:
  - ApiKeyAuth: []

paths:
  /health:
    get:
      summary: Health check
      security: []
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Health'

  /skills:
    get:
      summary: List skills library
      parameters:
        - name: niche
          in: query
          schema: { type: string }
          example: fintech
        - name: tags
          in: query
          schema: { type: string }
          example: dark-mode,minimalist
        - name: limit
          in: query
          schema: { type: integer, default: 50 }
        - name: offset
          in: query
          schema: { type: integer, default: 0 }
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SkillList'

  /analyze:
    post:
      summary: Analyze a live website
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AnalyzeRequest'
      responses:
        '200':
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UISkill'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '429':
          $ref: '#/components/responses/RateLimited'

components:
  securitySchemes:
    ApiKeyAuth:
      type: apiKey
      in: header
      name: x-api-key

  schemas:
    UISkill:
      type: object
      required: [domain, niche, layout, typography, color_language, design_principles, tags]
      properties:
        domain:
          type: string
          example: stripe.com
        niche:
          type: string
          example: Fintech / Payments
        design_principles:
          type: array
          items: { type: string }

    AnalyzeRequest:
      type: object
      required: [url]
      properties:
        url:
          type: string
          format: uri
          example: https://stripe.com
        page_type:
          type: string
          enum: [homepage, pricing, docs, landing, dashboard]
          default: homepage

  responses:
    BadRequest:
      description: Invalid request
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/Error'
    Unauthorized:
      description: Missing or invalid API key
    RateLimited:
      description: Rate limit exceeded
      headers:
        X-RateLimit-Reset:
          schema: { type: string, format: date-time }
```

---

## API Quality Checklist

**Design**
- [ ] All endpoints use nouns (plural), not verbs
- [ ] HTTP methods match semantics (GET=read, POST=create, DELETE=remove)
- [ ] Error responses follow consistent schema
- [ ] All 4xx errors return actionable messages

**Security**
- [ ] Auth required on all non-public endpoints
- [ ] Rate limiting on write/expensive endpoints
- [ ] helmet() middleware for security headers
- [ ] No sensitive data in error messages

**Developer Experience**
- [ ] Health endpoint with no auth
- [ ] Consistent pagination format
- [ ] X-RateLimit-* headers on rate-limited endpoints
- [ ] OpenAPI spec generated or written
- [ ] README with curl examples for every endpoint