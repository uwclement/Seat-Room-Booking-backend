# Use official OpenJDK image
FROM openjdk:17-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy Maven/Gradle wrapper and pom.xml first (for caching)
COPY mvnw .
COPY pom.xml .
COPY .mvn .mvn

# Install dependencies and build the project
RUN ./mvnw clean package -DskipTests

# Copy the source code
COPY src ./src

# Set environment variable for Railway port
ENV PORT=8080

# Expose the port
EXPOSE 8080

# Run the Spring Boot application
CMD ["java", "-jar", "target/your-app.jar"]
