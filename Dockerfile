# Use official JDK 21
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for caching)
COPY mvnw .
COPY pom.xml .
COPY .mvn .mvn

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the project (skip tests for faster build)
RUN ./mvnw clean package -DskipTests

# Copy source code
COPY src ./src

# Set Railway port
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot app
CMD ["java", "-jar", "target/library-booking-0.0.1-SNAPSHOT.jar"]
