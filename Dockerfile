# Using maven base image for building with maven.
FROM maven:latest AS builder

LABEL "repository"="https://github.com/ib-ai/modmail/"
LABEL "homepage"="https://discord.gg/IBO/"

WORKDIR /modmail/

# Resolve maven dependencies
COPY pom.xml .
COPY checkstyle.xml .
RUN mvn dependency:go-offline -B

# Build from source into ./target/*.jar
COPY src ./src
RUN mvn -e -B package

# Using Java JDK 10 base image
FROM openjdk:10

WORKDIR /modmail/

# Copying maven dependencies from builder image to prevent re-downloads
COPY --from=builder /root/.m2 /root/.m2

# Copying artifacts from maven (builder) build stage
COPY --from=builder /modmail/pom.xml .
COPY --from=builder /modmail/target/ ./target

# Running bot. Uses version from pom.xml to call artifact file name.
CMD VERSION="$(grep -oP -m 1 '(?<=<version>).*?(?=</version>)' /modmail/pom.xml)" && \
    java -jar /modmail/target/Modmail.jar