# HotelOS Authentication & Identity Architecture (P2.2)

## 1. Overview
The `identity-service` provides authentication and JWT issuance for HotelOS staff members. It authenticates users against BCrypt-hashed credentials stored in PostgreSQL (`identity.users`), enforces account lockout policies, logs audit events, and issues 15-minute RS256 access JSON Web Tokens (JWT).

## 2. RSA Key Pair Management

### Key Pair Storage & Security
- **Private Key**: `secrets/jwt-private.pem` (PKCS#8, mode `0600`) - **Restricted exclusively to `identity-service`**.
- **Public Key**: `secrets/jwt-public.pem` (X.509, mode `0644`) - Available to resource servers / gateway for signature verification.
- **Git Hygiene**: `secrets/`, `*.pem`, and `*.key` are strictly ignored by `.gitignore`. No private keys or production secrets are ever committed.

### Generating Local Development Keys
To generate a 2048-bit RSA key pair in the standard PKCS#8 format required by Java and Docker:

```bash
# 1. Create secrets directory if not present
mkdir -p secrets

# 2. Generate RSA 2048-bit private key (PKCS#8)
openssl genpkey -algorithm RSA -out secrets/jwt-private.pem -pkeyopt rsa_keygen_bits:2048

# 3. Extract public key (X.509)
openssl rsa -pubout -in secrets/jwt-private.pem -out secrets/jwt-public.pem

# 4. Restrict permissions
chmod 600 secrets/jwt-private.pem
chmod 644 secrets/jwt-public.pem
```

## 3. Login Endpoint & Access Token

### Endpoint Contract
- **Method**: `POST /api/auth/login`
- **Port**: `8086` (direct to `identity-service`)
- **Request**:
```json
{
  "username": "admin.staff",
  "password": "..."
}
```
- **Response (HTTP 200)**:
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```
- **Error Response (HTTP 401)**:
```json
{
  "error": "Unauthorized",
  "message": "Invalid username or password"
}
```
*(Identical generic message for non-existent users, wrong passwords, disabled accounts, and locked accounts).*

### RS256 JWT Specification
- **Algorithm**: RS256 (`SHA256withRSA`)
- **Type**: `JWT`
- **Key ID (`kid`)**: `hotelos-rsa-key-1`
- **Lifetime**: 900 seconds (15 minutes)
- **Claims**:
  - `iss`: `hotelos-identity`
  - `aud`: `hotelos-api`
  - `sub`: User UUID (e.g. `18b04775-38f7-4885-bd6f-00aa1ce76b39`)
  - `username`: Normalized username string
  - `roles`: Array of assigned roles, e.g. `["ADMIN"]`
  - `iat`: Epoch second issued
  - `exp`: Epoch second expires (`iat + 900`)
  - `jti`: Random UUID unique identifier

## 4. Account Lockout & Audit Policies
- **Failed Attempt Threshold**: 5 failed login attempts.
- **Lockout Duration**: 15 minutes (`NOW() + INTERVAL '15 minutes'`).
- **Atomic Tracking**: `failed_login_attempts` is updated atomically in `identity.users` within an independent database transaction (`Propagation.REQUIRES_NEW`), guaranteeing counter persistence even when 401 Unauthorized is returned.
- **Audit Logging**: Events `LOGIN_SUCCESS` and `LOGIN_FAILURE` are recorded in `identity.auth_audit_log` with client IP and reason without logging raw passwords.
