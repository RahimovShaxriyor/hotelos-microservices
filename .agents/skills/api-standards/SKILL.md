---
name: api-standards
description: Enforce REST API formatting, authentication schemas, and logging best practices.
---
# API & Middleware Standards
1. **Error Output**: API standard responses should always hide stack trace errors in production.
2. **Auth Layer**: Middleware should enforce consistent API key patterns (e.g., `prefix_<random_string>`).
3. **Rate Limiting**: Implement tiered limits based on API key levels (e.g., Free, Pro, Admin).
4. **Logging Constraints**: Use `console.error()` for internal errors. Logging systems must enforce NO payload logging (PII or keys). Use `stderr` strictly for logs to prevent MCP stdio stream corruption.
