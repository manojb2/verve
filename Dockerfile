# Use an Ubuntu base image for the build stage
FROM ubuntu:22.04 AS build

# Set environment variables for Java and Maven
#ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

# Install Java 17, Maven, wget, vim, and curl
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk maven wget vim curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application, skipping tests
RUN mvn clean package -DskipTests

# Use a slim Ubuntu base image for the runtime
FROM ubuntu:22.04

# Install Java 17 runtime
RUN apt-get update && \
    apt-get install -y openjdk-17-jre curl wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set the environment variable for Java
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Copy the packaged JAR file from the build stage
COPY --from=build /app/target/verve-0.0.1-SNAPSHOT.jar /app.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
