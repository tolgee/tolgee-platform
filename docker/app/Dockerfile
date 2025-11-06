FROM postgres:18.0

ENTRYPOINT []

RUN <<EOF
  set -eux
  apt -qq update
  apt -qq -y install wget
  wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
  echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
  apt -qq update
  apt -qq -y install temurin-21-jdk libxml2
  apt -qq -y remove wget --purge --auto-remove
  rm -rf /var/lib/apt/lists/*
EOF

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
COPY --chmod=755 cmd.sh /app

#################
### Let's go   ##
#################

# Define the startup command
ENTRYPOINT ["/app/cmd.sh"]


# Health check to ensure the app is up and running
HEALTHCHECK --interval=10s --timeout=3s --retries=20 \
    CMD wget --spider -q "http://127.0.0.1:$HEALTHCHECK_PORT/actuator/health" || exit 1
