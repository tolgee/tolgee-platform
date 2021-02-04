FROM openjdk:14-jdk-alpine
VOLUME /tmp

ARG DEPENDENCY=target/dependency
ARG PROPERTIES=application.yaml

EXPOSE 8080

VOLUME /db

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
COPY ${DEPENDENCY}/${PROPERTIES} /app/application.yaml

ENTRYPOINT ["java","-cp","app:app/lib/*","io.tolgee.Application"]
