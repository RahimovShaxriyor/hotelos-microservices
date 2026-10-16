---
name: security-hardening
description: Implement industry-standard security practices and mitigate web vulnerabilities. Use this skill when configuring security headers (CSP, HSTS), implementing secure authentication flows, managing session security (JWT, HttpOnly cookies), sanitizing user input, mitigating OWASP Top 10 risks (XSS, CSRF, SQLi), or setting up rate limiting and WAF patterns. Triggers on: "security hardening", "OWASP Top 10", "XSS mitigation", "CSRF protection", "secure cookies", "JWT security", "CSP header", "sanitization", "rate limiting", "WAF", "security audit", "penetration testing", "HSTS", "CORS policy". Always use this skill when building production-grade web applications.
---

# Security Hardening Skill

Protect your users and data with a defense-in-depth security architecture. This skill focuses on the practical implementation of OWASP-compliant security controls.

---

## The Secure-by-Default Mental Model

Security is not a plugin; it's a foundation. Implement these four layers of defense:

1. **Protocol Security**: HSTS, SSL/TLS certificates, and secure redirects.
2. **Identity Security**: Robust auth, secure sessions, and MFA.
3. **Application Security**: Input validation, output encoding, and access control.
4. **Header Security**: CSP, Frame Options, and XSS Protection.

---

## Protecting the Session

### 1. Secure Cookie Pattern
Always use `HttpOnly`, `Secure`, and `SameSite` flags.

```javascript
// Example Express cookie config
res.cookie('session_id', id, {
  httpOnly: true, // Prevents XSS-based cookie theft
  secure: true,   // Only sent over HTTPS
  sameSite: 'strict', // Prevents CSRF
  maxAge: 3600000 // 1 hour
});
```

### 2. JWT (JSON Web Token) Security
- **Algorithm**: Always use `RS256` (Asymmetric) in production.
- **Short TTL**: Access tokens should live < 15 minutes.
- **Refresh Flow**: Use a separate refresh token stored in an `HttpOnly` cookie.

---

## Security Headers: The Defensive Shield

### 1. Content Security Policy (CSP)
The most powerful tool against XSS. Start strict and loosen only if needed.

```http
Content-Security-Policy: default-src 'self'; script-src 'self' https://trusted.cdn.com; img-src 'self' data:; style-src 'self' 'unsafe-inline';
```

### 2. Common Security Headers (via Helmet.js)

```javascript
import helmet from 'helmet';

// Set essential headers automatically
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      "default-src": ["'self'"],
      "script-src": ["'self'", "trusted-scripts.com"],
      "object-src": ["'none'"],
      "upgrade-insecure-requests": [],
    },
  },
  xssFilter: true, // X-XSS-Protection: 1; mode=block
  noSniff: true,   // X-Content-Type-Options: nosniff
  hsts: { maxAge: 31536000, includeSubDomains: true, preload: true }, // Strict-Transport-Security
}));
```

---

## OWASP Mitigation Patterns

### 1. XSS (Cross-Site Scripting)
- **Rule**: Never trust user input. Use libraries like `DOMPurify` for HTML.
- **Rule**: React/Vue escape content by default, but be careful with `dangerouslySetInnerHTML`.

```javascript
import DOMPurify from 'dompurify';
const cleanHTML = DOMPurify.sanitize(dirtyHTML);
```

### 2. CSRF (Cross-Site Request Forgery)
- **Prevention**: Use `SameSite: Strict` cookies.
- **Prevention**: For non-browser clients (APIs), the `Authorization: Bearer` header is immune to CSRF.
- **Prevention**: For browser-to-server posts, use a CSRF token.

### 3. SQL Injection (SQLi)
- **Rule**: Never use string interpolation for queries. Always use **Parameterized Queries**.

```javascript
// ✅ Correct (Parameterized)
const user = db.prepare('SELECT * FROM users WHERE id = ?').get(id);

// ❌ Incorrect (Vulnerable)
const user = db.query(`SELECT * FROM users WHERE id = ${id}`);
```

---

## Rate Limiting & Brute Force Prevention

Implement tiered rate limits and account lockout policies.

```javascript
import rateLimit from 'express-rate-limit';

const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 5, // 5 attempts per window
  message: 'Too many login attempts. Please try again after 15 minutes.'
});

app.use('/auth/login', loginLimiter);
```

---

## Security Audit Checklist

- [ ] **Auth**: Is MFA available for sensitive accounts?
- [ ] **Secrets**: Are `API_KEYS` and `DB_URL` in `.env`, not in code?
- [ ] **Dependencies**: Is `npm audit` part of the CI pipeline?
- [ ] **Errors**: Are stack traces hidden in production (log to stderr instead)?
- [ ] **Scanning**: Using tools like Snyk or GitHub Dependabot?
- [ ] **Data**: Is PII (Personally Identifiable Information) encrypted at rest?
- [ ] **Permissions**: Is the API using Least Privilege (e.g., restricted API keys)?

---

## Reference Resources

- [OWASP Top 10 Project](https://owasp.org/www-project-top-ten/)
- [Mozilla Observatory](https://observatory.mozilla.org/)
- [Security Headers Scanner](https://securityheaders.com/)
