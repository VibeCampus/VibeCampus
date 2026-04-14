# 仅打包已构建的前端静态资源；构建上下文内需有 default.conf 与 dist/（见文档）
FROM nginx:1.27-alpine
COPY default.conf /etc/nginx/conf.d/default.conf
COPY dist/ /usr/share/nginx/html/
