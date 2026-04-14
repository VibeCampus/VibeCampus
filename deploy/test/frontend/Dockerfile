# 构建上下文：仓库根目录（与 docker-compose 中 web.build.context 一致）
FROM node:22-bookworm-slim AS build
WORKDIR /app
COPY VibeCampus-Frontend/package.json VibeCampus-Frontend/package-lock.json ./
RUN npm ci
COPY VibeCampus-Frontend/ ./
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/test/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
