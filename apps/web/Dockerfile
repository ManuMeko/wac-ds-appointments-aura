# syntax=docker/dockerfile:1

# Node 22.23.2, la misma versión que en local.
# npm ci instala este lock en Windows y lo rechaza en Linux:
# picomatch 2.3.2 no satisface el 4.0.7 anidado.
# npm install no añade dependencias directas; next y react siguen fijados.
FROM node:22.23.2-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm install --no-audit --no-fund

FROM node:22.23.2-alpine AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# La aplicación lee NEXT_PUBLIC_API_BASE_URL.
# NEXT_PUBLIC_API_URL es el build arg de despliegue y se copia a ese nombre.
ARG NEXT_PUBLIC_DEMO=aura
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_SITE_URL
ARG NEXT_IMAGE_UNOPTIMIZED=true

ENV NEXT_PUBLIC_DEMO=$NEXT_PUBLIC_DEMO
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
ENV NEXT_IMAGE_UNOPTIMIZED=$NEXT_IMAGE_UNOPTIMIZED
ENV NEXT_TELEMETRY_DISABLED=1

RUN test -n "$NEXT_PUBLIC_DEMO" \
    && test -n "$NEXT_PUBLIC_API_URL" \
    && test -n "$NEXT_PUBLIC_SITE_URL" \
    && npm run build

FROM node:22.23.2-alpine AS run

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN addgroup -g 1001 -S nodejs \
    && adduser -S -u 1001 -G nodejs nextjs

COPY --from=build --chown=nextjs:nodejs /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

CMD ["node", "server.js"]
