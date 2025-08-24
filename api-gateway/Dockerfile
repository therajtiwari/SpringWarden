# Stage 1: Use the official OpenJDK 21 image as the base.
FROM eclipse-temurin:21-jdk-jammy

# Set the working directory inside the container.
WORKDIR /app

# Copy the compiled JAR file from your local 'target' directory.
COPY target/api-gateway-1.0.0.jar app.jar

# Expose the public-facing port for the API Gateway.
EXPOSE 8080

# The command to run the application when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]
