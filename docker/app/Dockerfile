FROM tolgee/base:jdk-14-postgres-13

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
CMD ["ash", "-c", "java -cp app:app/lib/* io.tolgee.Application"]

HEALTHCHECK --interval=10s --timeout=3s --retries=2 CMD wget -q -O -  http://127.0.0.1:8080/actuator/health | sed -n "/UPa/p;q1" || exit 1
