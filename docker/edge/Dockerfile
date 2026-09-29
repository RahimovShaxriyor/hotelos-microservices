FROM nginx:alpine

# Remove default configuration
RUN rm -f /etc/nginx/conf.d/default.conf

# Copy configurations and entrypoint
COPY docker/edge/nginx.conf /etc/nginx/nginx.conf
COPY docker/edge/includes/ /etc/nginx/includes/
COPY docker/edge/hotelos-plain.conf.template /etc/nginx/conf.d/hotelos-plain.conf.template
COPY docker/edge/hotelos-ssl.conf.template /etc/nginx/conf.d/hotelos-ssl.conf.template
COPY docker/edge/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh && \
    mkdir -p /etc/nginx/certs /var/log/nginx /var/cache/nginx /tmp && \
    chmod -R 777 /var/log/nginx /var/cache/nginx /tmp

EXPOSE 80 443

ENTRYPOINT ["/entrypoint.sh"]
