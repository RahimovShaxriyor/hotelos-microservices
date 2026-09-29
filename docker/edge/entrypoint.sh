#!/bin/sh
set -e

# If TLS certificates exist in /etc/nginx/certs/, activate SSL configuration
if [ -f "/etc/nginx/certs/tls.crt" ] && [ -f "/etc/nginx/certs/tls.key" ]; then
    echo "[hotelos-edge] TLS certificate and key detected. Enabling HTTPS/WSS with HTTP->HTTPS redirect."
    cp /etc/nginx/conf.d/hotelos-ssl.conf.template /etc/nginx/conf.d/default.conf
else
    echo "[hotelos-edge] No TLS certificates detected. Running in plain HTTP/WS mode."
    cp /etc/nginx/conf.d/hotelos-plain.conf.template /etc/nginx/conf.d/default.conf
fi

if [ $# -gt 0 ]; then
    exec "$@"
fi

exec nginx -g "daemon off;"
