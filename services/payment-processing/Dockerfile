# Dockerfile para el módulo: payment-processing
# Ubicar este archivo en: services/payment-processing/Dockerfile
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

FROM eclipse-temurin:17-jre AS runtime

# curl lo usa el healthcheck de docker-compose
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario no root; logs/ se crea con ownership antes de cambiar de usuario
RUN groupadd --system appgroup && useradd --system --gid appgroup --no-create-home appuser
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs
USER appuser
WORKDIR /app

COPY --from=builder /build/services/payment-processing/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]