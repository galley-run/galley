# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the fat jar from build stage
COPY --from=build /app/target/galley-*-fat.jar /app/galley.jar

# Expose port
EXPOSE 443

# Run the application
ENTRYPOINT ["sh", "-c", "java -jar /app/galley.jar -conf ${CONF_JSON_PATH:-/app/conf.json}"]
