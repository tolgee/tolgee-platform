# Tolgee Base Docker Image
This Docker image contains JDK and Postgres to run Tolgee.

## It is published manually to DockerHub
To build it and publish run:

    docker build . -t tolgee/base:jdk-14-postgres-13
    docker push tolgee/base:jdk-14-postgres-13

