FROM postgres:13.23-alpine3.22

ENTRYPOINT []

RUN apk --no-cache upgrade \
    busybox=1.37.0-r20 \
    libcrypto3=3.5.5-r0 \
    libssl3=3.5.5-r0

RUN apk --no-cache add openjdk21=21.0.10_p7-r0
RUN apk --no-cache add libxml2=2.13.9-r0

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

# Download OpenTelemetry Java agent for distributed tracing
# When OTEL_JAVAAGENT_ENABLED=true, cmd.sh will use this agent
# See: docs/observability/
# Version is passed from gradle.properties via --build-arg (see gradle/docker.gradle)
ARG OTEL_AGENT_VERSION
RUN test -n "$OTEL_AGENT_VERSION" || (echo "OTEL_AGENT_VERSION build arg is required" && exit 1)
RUN wget -O /app/opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"

#################
### Let's go   ##
#################

# Define the startup command
ENTRYPOINT ["/app/cmd.sh"]


# Health check to ensure the app is up and running
HEALTHCHECK --interval=10s --timeout=3s --retries=20 \
    CMD wget --spider -q "http://127.0.0.1:$HEALTHCHECK_PORT/actuator/health" || exit 1
