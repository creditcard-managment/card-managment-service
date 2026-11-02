# Dockerfile para el módulo: payment-processing
# Ubicar este archivo en: payment-processing/Dockerfile
# Build context debe ser la raíz del proyecto

FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
COPY card-data/pom.xml          card-data/
COPY card-events/pom.xml        card-events/
COPY card-lifecycle/pom.xml     card-lifecycle/
COPY card-limits/pom.xml        card-limits/
COPY card-shared/pom.xml        card-shared/
COPY payment-processing/pom.xml payment-processing/

RUN mvn dependency:go-offline -B --no-transfer-progress

COPY . .

RUN mvn clean package -DskipTests -B --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine AS runtime

RUN apk add --no-cache curl


RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs
USER appuser
WORKDIR /app

COPY --from=builder /build/payment-processing/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]