FROM tolgee/base:jdk-17-postgres-13

#############
### Tolgee  #
#############

EXPOSE 8080

VOLUME /data

COPY BOOT-INF/lib /app/lib
COPY META-INF /app/META-INF
COPY BOOT-INF/classes /app

#################
### Let's go   ##
#################

ENV spring_profiles_active docker
CMD ["java", "-cp", "app:app/lib/*", "io.tolgee.Application"]

ENV HEALTHCHECK_PORT=8080

HEALTHCHECK --interval=10s --timeout=3s --retries=20 \
    CMD [[ "$(wget -q -O -  http://127.0.0.1:$HEALTHCHECK_PORT/actuator/health)" == *"UP"* ]] || exit 1
