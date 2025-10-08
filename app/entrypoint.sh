#!/bin/sh
set -eux

# Ensure Java is on PATH
export JAVA_HOME="${JAVA_HOME:-/opt/java/openjdk}"
export PATH="$JAVA_HOME/bin:$PATH"

# Generate a JKS keystore on first start (dev default)
if [ ! -f /opt/tim/jwtkeystore.jks ]; then
  keytool -genkeypair -alias jwtsign -keyalg RSA -keysize 2048     -keystore /opt/tim/jwtkeystore.jks -storepass "${KEY_PASS:-changeme}"     -dname "CN=demo, OU=, O=, L=, ST=, C=" -validity 3650
fi

# Run Spring Boot with explicit system properties (DB + JWT keystore)
exec java   -Dserver.port=8085   -Dspring.datasource.url="${SPRING_DATASOURCE_URL:-jdbc:postgresql://postgres:5432/tim}"   -Dspring.datasource.username="${SPRING_DATASOURCE_USERNAME:-tim}"   -Dspring.datasource.password="${SPRING_DATASOURCE_PASSWORD:-123}"   -Dspring.jpa.hibernate.ddl-auto=none   -Djwt.signature.key-store="file:/opt/tim/jwtkeystore.jks"   -Djwt.signature.key-store-type="JKS"   -Djwt.signature.key-store-password="${KEY_PASS:-changeme}"   -Djwt.signature.key-alias="jwtsign"   -jar /opt/tim/app.jar
