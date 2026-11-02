# Dockerfile para el módulo: card-limits
# Ubicar este archivo en: services/card-limits/Dockerfile
# Build context debe ser la raíz del proyecto

FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
COPY services/card-data/pom.xml          services/card-data/
COPY services/card-events/pom.xml        services/card-events/
COPY services/card-lifecycle/pom.xml     services/card-lifecycle/
COPY services/card-limits/pom.xml        services/card-limits/
COPY services/card-shared/pom.xml        services/card-shared/
COPY services/payment-processing/pom.xml services/payment-processing/

RUN mvn dependency:go-offline -B --no-transfer-progress

COPY . .

RUN mvn clean package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine AS runtime

RUN apk add --no-cache curl


RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs
USER appuser
WORKDIR /app

COPY --from=builder /build/services/card-limits/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
