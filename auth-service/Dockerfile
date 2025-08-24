# Stage 1: Use the official OpenJDK 17 image as the base.
# The "slim" variant is a good balance of size and functionality.
FROM eclipse-temurin:21-jdk-jammy

# Set the working directory inside the container. All subsequent commands will run from here.
WORKDIR /app

# Copy the compiled JAR file from your local 'target' directory into the container.
# The JAR name must match the <artifactId>-<version>.jar defined in the pom.xml.
COPY target/auth-service-1.0.0.jar app.jar

# Expose the port that the application runs on. This is for documentation and can be used by Docker networking.
EXPOSE 8081

# The command to run when the container starts.
# Using the "exec" form ensures that the Java process is the main process (PID 1),
# which allows it to receive signals like SIGTERM for graceful shutdown.
ENTRYPOINT ["java", "-jar", "app.jar"]