FROM tolgee/base:jdk-21-postgres-13

#############
### Tolgee  #
#############

# Expose application port
EXPOSE 8080

# Define persistent volume for data storage
VOLUME /data

# Environment variables for configuration
ENV HEALTHCHECK_PORT=8080 \
    spring_profiles_active=docker

# Copy necessary application files
COPY BOOT-INF/lib /app/lib
COPY META-INF /app/META-INF
COPY BOOT-INF/classes /app

#################
### Let's go   ##
#################

# Define the startup command
CMD ["java", "-cp", "app:app/lib/*", "io.tolgee.Application"]

# Health check to ensure the app is up and running
HEALTHCHECK --interval=10s --timeout=3s --retries=20 \
    CMD curl -f http://127.0.0.1:$HEALTHCHECK_PORT/actuator/health || exit 1
