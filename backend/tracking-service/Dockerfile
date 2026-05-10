FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json ./
# pnpm-lock.yaml will be added when you commit; until then, npm i is fine.
RUN --mount=type=cache,target=/root/.npm npm install --omit=dev --no-audit --no-fund

FROM node:20-alpine AS build
WORKDIR /app
COPY package.json tsconfig.json tsconfig.build.json ./
COPY src ./src
RUN --mount=type=cache,target=/root/.npm npm install --no-audit --no-fund
RUN npx tsc -p tsconfig.build.json

FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S tms && adduser -S -G tms -u 1001 tms
COPY --from=deps /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY package.json ./
USER 1001
EXPOSE 8004
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s \
    CMD wget -qO- http://localhost:8004/actuator/health || exit 1
CMD ["node", "dist/main.js"]
