#!/usr/bin/env bash
#
# Tolgee startup script with optional OpenTelemetry tracing support.
#
# Environment variables:
#   OTEL_JAVAAGENT_ENABLED=true - Enable OpenTelemetry Java agent for tracing
#
# See: docs/observability/

ARCH=$(uname -m)
JAVA_OPTS_ARRAY=()

# Preserve any externally-provided JAVA_OPTS
if [ -n "$JAVA_OPTS" ]; then
    # Note: This simple approach handles space-separated options but not quoted values with spaces
    read -ra JAVA_OPTS_ARRAY <<< "$JAVA_OPTS"
fi

# Architecture-specific options
# There is an issue with M4 arm processors, explained here:
# https://github.com/corretto/corretto-21/issues/85
if [ "$ARCH" = "aarch64" ]; then
    JAVA_OPTS_ARRAY+=("-XX:UseSVE=0")
fi
# CVE-2026-114938: the AVX-512 Base64 decode intrinsic in OpenJDK < 25.0.5.1 has a heap overflow
# reachable before authentication (JWT, Basic auth). Keep this until the image ships a fixed JRE.
if [ "$ARCH" = "x86_64" ]; then
    JAVA_OPTS_ARRAY+=("-XX:+UnlockDiagnosticVMOptions" "-XX:-UseBASE64Intrinsics")
fi

# OpenTelemetry agent (enabled via OTEL_JAVAAGENT_ENABLED=true)
if [ "$OTEL_JAVAAGENT_ENABLED" = "true" ]; then
    JAVA_OPTS_ARRAY+=("-javaagent:/app/opentelemetry-javaagent.jar")
fi

exec java "${JAVA_OPTS_ARRAY[@]}" -cp "app:app/lib/*" io.tolgee.Application
