FROM openjdk:14-jdk-alpine

EXPOSE 8080

VOLUME /data

COPY BOOT-INF/lib /app/lib
COPY META-INF /app/META-INF
COPY BOOT-INF/classes /app
COPY application.yaml /app/application.yaml

ENTRYPOINT ["java","-cp","app:app/lib/*","io.tolgee.Application"]
