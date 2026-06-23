# ---- build stage ----
# Use a Maven + JDK 21 image so the build is self-contained and layer-cacheable.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first (cached unless pom.xml changes) for fast rebuilds.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Then build. Tests run in CI, not in the image build.
COPY src/ src/
RUN mvn -B -q -DskipTests package

# ---- runtime stage ----
# A JRE-only base keeps the image small and the attack surface low.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user (least privilege).
RUN groupadd --system spring && useradd --system --gid spring spring

# Copy the built jar (glob tolerates version bumps in the artifact name).
COPY --from=build /workspace/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
