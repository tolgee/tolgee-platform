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
