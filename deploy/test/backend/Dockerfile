# 构建上下文：仓库根目录（与 docker-compose 中 backend.build.context 一致）
FROM maven:3.9.9-eclipse-temurin-21 AS deps
WORKDIR /build

COPY deploy/test/backend/settings.xml /root/.m2/settings.xml
COPY VibeCampus-Backend/pom.xml ./pom.xml
COPY VibeCampus-Backend/vibecampus-common/pom.xml vibecampus-common/pom.xml
COPY VibeCampus-Backend/vibecampus-user/pom.xml vibecampus-user/pom.xml
COPY VibeCampus-Backend/vibecampus-post/pom.xml vibecampus-post/pom.xml
COPY VibeCampus-Backend/vibecampus-comment/pom.xml vibecampus-comment/pom.xml
COPY VibeCampus-Backend/vibecampus-admin/pom.xml vibecampus-admin/pom.xml
COPY VibeCampus-Backend/vibecampus-bootstrap/pom.xml vibecampus-bootstrap/pom.xml
RUN mvn -B -pl vibecampus-bootstrap -am dependency:go-offline

FROM deps AS builder
COPY VibeCampus-Backend/ ./
RUN mvn -B -pl vibecampus-bootstrap -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/vibecampus-bootstrap/target/vibecampus-bootstrap-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
