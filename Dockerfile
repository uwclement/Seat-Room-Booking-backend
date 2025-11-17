# Use official JDK 21
FROM eclipse-temurin:21-jdk-alpine

# Install bash and git (needed for Maven wrapper sometimes)
RUN apk add --no-cache bash git

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for caching)
COPY mvnw .
COPY pom.xml .
COPY .mvn .mvn

# Copy the source code BEFORE running build
COPY src ./src

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the project (skip tests)
RUN ./mvnw clean package -DskipTests

# Set Railway port
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot app
CMD ["java", "-jar", "target/library-booking-0.0.1-SNAPSHOT.jar"]
