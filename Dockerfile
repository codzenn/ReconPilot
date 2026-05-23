FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
COPY ui ./ui
RUN mvn -q test package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd -r -u 10001 appuser
COPY --from=build /workspace/target/reconcileguard-1.0.0.jar /app/reconcileguard.jar
ENV RG_PORT=8080
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "/app/reconcileguard.jar"]
