---
name: docker-playwright
description: Docker constraints and best practices when managing Playwright browser image builds and deployments.
---
# Playwright Docker Management
1. **Base Image**: Always deploy using `FROM mcr.microsoft.com/playwright`.
2. **Flags**: Set `NODE_OPTIONS=--max-old-space-size=1024` for the memory environment variable.
3. **Builds**: Do not run any raw CDN Chromium downloads during pipeline builds; use the pre-baked browsers in the Playwright image.
