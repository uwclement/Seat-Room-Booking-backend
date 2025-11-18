# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the project
RUN ./mvnw clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the jar from build stage
COPY --from=build /app/target/library-booking-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Railway will override with PORT)
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java","-jar","app.jar"]
