# Step 1: Use an official Java runtime as a parent image
FROM eclipse-temurin:17-jre-alpine

# Step 2: Set the working directory inside the container
WORKDIR /app

# Step 3: Copy the executable JAR file from your target folder to the container
# Note: You must run 'mvn clean package' on your computer before building this
COPY target/*.jar app.jar

# Step 4: Copy the secrets file so the app can find it
COPY src/main/resources/application-secrets.properties src/main/resources/application-secrets.properties

# Step 5: Expose the port your app runs on
EXPOSE 8080

# Step 6: Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
