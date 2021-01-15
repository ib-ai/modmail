# Using Java JDK 10 base image
FROM openjdk:10

# Metadata
LABEL "repository"="https://github.com/ib-ai/modmail/"
LABEL "homepage"="https://discord.gg/IBO/"

WORKDIR /modmail/

# Add language files
COPY lang ./lang

# Download the latest binary from the CI
ADD https://ci.arraying.de/job/mm/lastSuccessfulBuild/artifact/target/Modmail.jar .

# Run the jar as a CMD so it can be overwritten in the run
# Could be useful for overwriting start parameters to, for example, restrict memory usage
CMD ["java", "-jar", "Modmail.jar"]
