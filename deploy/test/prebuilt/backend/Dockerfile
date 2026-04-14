# 仅打包已构建的 JAR；构建上下文目录内需有 app.jar（见文档「本机构建 + 上传」）
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
