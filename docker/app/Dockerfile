FROM openjdk:14-jdk-alpine
VOLUME /tmp

ARG DEPENDENCY=target/dependency
ARG PROPERTIES=application.properties

EXPOSE 8080

VOLUME /db

COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
COPY ${DEPENDENCY}/${PROPERTIES} /app/application.properties

#remove all profile properties
RUN rm -f /app/application-*.properties

ENTRYPOINT ["java","-cp","app:app/lib/*","io.polygloat.Application"]
