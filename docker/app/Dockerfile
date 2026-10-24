FROM eclipse-temurin:25-jre-alpine AS jre

# The JDK trust store still trusts the emSign/eMudhra roots, whose intermediate
# CA keys were compromised (October 2026). Remove the roots so the JVM rejects
# certificates chained to them. Drop this workaround once the base image ships
# Temurin 25.0.5+8 or later, which distrusts them itself (the delete then fails
# the build if the aliases are gone, which is the signal to remove this block).
RUN for alias in \
      'cn_emsign_ecc_root_ca__c3,o_emudhra_inc,ou_emsign_pki,c_us [jdk]' \
      'cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]' \
      'cn_emsign_root_ca__c1,o_emudhra_inc,ou_emsign_pki,c_us [jdk]' \
      'cn_emsign_root_ca__g1,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]'; \
    do keytool -delete -cacerts -storepass changeit -alias "$alias" || exit 1; done \
    && ! keytool -list -cacerts -storepass changeit | grep -iE 'emsign|emudhra'

# The postgres base image ships /usr/local/bin/gosu. Security scanners (Snyk)
# flag Go stdlib CVEs in that binary. They are false positives: gosu only drops
# privileges and execs, and never calls the flagged APIs (HTTP, TLS, archives).
# Our cmd.sh does not use gosu at all. There is also no fixed build to move to:
# the latest gosu release ships the same Go stdlib, and gosu's SECURITY.md says
# the maintainer does not rebuild for CVEs that do not apply. For this reason
# the Snyk project for this binary is deactivated.
FROM postgres:13.23-alpine3.22

ENTRYPOINT []

# Pull the latest patched Alpine packages (openssl, libxml2, ...) on top of the
# pinned base image, which lags behind the alpine3.22 branch on security fixes.
RUN apk --no-cache upgrade

# Alpine 3.22 only packages openjdk21, so bring the JRE 25 from the Temurin image.
# Both are musl/alpine builds, so the copied runtime is ABI-compatible.
COPY --from=jre /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:${PATH}"

RUN apk --no-cache add libxml2

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
COPY cmd.sh /app
RUN chmod 755 /app/cmd.sh

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
