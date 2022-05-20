# Tolgee Base Docker Image
This Docker image contains JDK and Postgres to run Tolgee.

## It is published manually to DockerHub
To build it and publish run:

    docker buildx build . -t tolgee/base:jdk-14-postgres-13 --platform linux/arm64,linux/amd64 --push

