# ---- build stage: compile + package with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# bound the Maven/compiler heap so constrained Docker VMs don't OOM
ENV MAVEN_OPTS="-Xmx768m"
# cache dependencies against pom.xml first
COPY pom.xml ./
# warm the dependency cache (best-effort; package below fetches anything missing)
RUN mvn -B -q dependency:go-offline || true
# then build
COPY src ./src
RUN mvn -B -q -DskipTests clean package

# ---- run stage: slim JRE (multi-arch Ubuntu) ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/storage
COPY --from=build /app/target/msj-backend.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=70s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
