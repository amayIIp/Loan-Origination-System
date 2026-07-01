# ---------------------------------------------------------
# STAGE 1: BUILD (Compiles the Java code into a JAR file)
# ---------------------------------------------------------
# We use a heavy "Maven" image that contains all the tools needed to build Java code.
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set the working directory inside the container.
WORKDIR /app

# Copy the standard Maven configuration file so we can download dependencies.
COPY pom.xml .

# Download dependencies first (this step gets cached by Docker to speed up future builds).
RUN mvn dependency:go-offline -B

# Copy our actual Java source code into the container.
COPY src ./src

# Compile the code and package it into a runnable .jar file, skipping tests (tests run in CI).
RUN mvn clean package -DskipTests

# ---------------------------------------------------------
# STAGE 2: RUNTIME (Runs the built application)
# ---------------------------------------------------------
# We switch to a "slim" JRE (Java Runtime Environment) image. It only has what's needed to RUN Java, 
# not build it. This makes the final Docker image much smaller and more secure.
FROM eclipse-temurin:17-jre-alpine

# Set the working directory for the runtime container.
WORKDIR /app

# Copy the built .jar file from the "builder" stage above into this runtime stage.
COPY --from=builder /app/target/*.jar app.jar

# Define JVM memory flags tailored for small cloud instances (e.g., 512MB RAM total).
# -Xms256m: Start with 256MB of memory.
# -Xmx256m: Never use more than 256MB of memory to prevent OutOfMemory crashes in Docker.
ENV JAVA_OPTS="-Xms256m -Xmx256m"

# Tell Docker to expose port 8080 so external traffic can reach our Spring Boot app.
EXPOSE 8080

# The command that actually starts our Spring Boot application when the container boots.
# We wrap it in a shell to ensure the JAVA_OPTS environment variable is read correctly.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
