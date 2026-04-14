# 构建上下文：仓库根目录（与 docker-compose 中 backend.build.context 一致）
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY VibeCampus-Backend/ ./
RUN mvn -pl vibecampus-bootstrap -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/vibecampus-bootstrap/target/vibecampus-bootstrap-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
