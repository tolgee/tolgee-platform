# Tolgee Base Docker Image
This Docker image contains JDK and Postgres to run Tolgee.

## It is published manually to DockerHub
To build it and publish run:

    docker buildx build . -t tolgee/base:jdk-21-postgres-13 --platform linux/arm64,linux/amd64 --push


## Troubleshooting

Sometimes it fails, with 

    ERROR: Multiple platforms feature is currently not supported for docker driver. Please switch to a different driver (eg. "docker buildx create --use")

Which can be solved by creating a new builder:

    docker buildx create --platform linux/arm64,linux/amd64 --use

