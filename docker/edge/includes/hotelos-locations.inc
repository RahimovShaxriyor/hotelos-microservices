# Security Headers
add_header X-Content-Type-Options nosniff always;
add_header X-Frame-Options DENY always;
add_header Referrer-Policy strict-origin-when-cross-origin always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; connect-src 'self' ws: wss:; frame-ancestors 'none';" always;

# Trusted proxy headers: override client-supplied values to prevent header spoofing
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $remote_addr;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Port $server_port;

# Swagger / OpenAPI lockdown for production
location /swagger-ui/ {
    return 404;
}
location = /swagger-ui.html {
    return 404;
}
location /v3/api-docs/ {
    return 404;
}
location = /v3/api-docs {
    return 404;
}

# Actuator lockdown
location /actuator/ {
    return 404;
}

# Sensitive authentication endpoints with rate limiting
location = /api/auth/login {
    limit_req zone=auth_limit burst=5 nodelay;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_pass http://gateway-backend;
}

location = /api/auth/refresh {
    limit_req zone=auth_limit burst=5 nodelay;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_pass http://gateway-backend;
}

location = /api/ws/tickets/dashboard {
    limit_req zone=ticket_limit burst=10 nodelay;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_pass http://gateway-backend;
}

# General REST API routing to Gateway
location /api/ {
    proxy_pass http://gateway-backend;
}

# WebSocket endpoint with protocol upgrade and long-lived connection support
location = /ws/dashboard {
    proxy_pass http://dashboard-backend/ws/dashboard;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $remote_addr;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}

# Dashboard Web UI routing
location / {
    proxy_pass http://dashboard-backend;
}
