# Use a Maven base image to build the application
FROM maven:3.9.5-amazoncorretto-17 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application (skipping tests)
RUN mvn package -DskipTests

# Use a smaller, lighter image to run the application
FROM amazoncorretto:17-alpine-jdk

# Set the working directory
WORKDIR /app

# Copy the packaged jar from the build stage
COPY --from=build /app/target/digital-consent-tracker-0.0.1-SNAPSHOT.jar .

COPY --from=build /app/target/classes/static/dist/ /app/BOOT-INF/classes/static/
# Expose the application port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "digital-consent-tracker-0.0.1-SNAPSHOT.jar"]
