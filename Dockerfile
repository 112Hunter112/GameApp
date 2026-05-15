# --- STAGE 1: Build the App (Maven) ---
# We use a Maven image to compile the code
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the pom.xml first (to cache dependencies)
COPY pom.xml .
# Download dependencies (this step is cached if pom.xml doesn't change)
RUN mvn dependency:go-offline

# Copy the source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# --- STAGE 2: Run the App (Java Runtime) ---
# We use a lightweight Alpine image for the final container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root for defense-in-depth
RUN adduser -D spring
USER spring

# Copy the JAR file built in Stage 1 (avoid *.jar which also matches .jar.original)
COPY --from=build /app/target/SportsBackend-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseContainerSupport", "-jar", "app.jar"]
