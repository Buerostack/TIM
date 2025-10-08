# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache maven
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY custom-jwt/pom.xml custom-jwt/pom.xml
COPY oauth2-oidc/pom.xml oauth2-oidc/pom.xml
COPY server/pom.xml server/pom.xml
RUN mvn -q -DskipTests dependency:go-offline
COPY common common
COPY custom-jwt custom-jwt
COPY oauth2-oidc oauth2-oidc
COPY server server
RUN mvn -q -DskipTests -pl server -am package

# ---- Runtime stage (Debian JDK for stable PATH/Java) ----
FROM eclipse-temurin:17-jdk
WORKDIR /opt/tim
ENV KEY_PASS=changeme
COPY --from=build /app/server/target/*.jar /opt/tim/app.jar
COPY entrypoint.sh /opt/tim/entrypoint.sh
RUN chmod +x /opt/tim/entrypoint.sh
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
EXPOSE 8085
ENTRYPOINT ["/bin/sh", "/opt/tim/entrypoint.sh"]
