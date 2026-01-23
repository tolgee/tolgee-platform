# Observability in Tolgee

## What We Need

Three capabilities for debugging and monitoring:

1. **Distributed Tracing** - Follow a request across service boundaries, see where time is spent
2. **Log Correlation** - Jump from a trace to its logs, or from a log line to its parent trace
3. **Metrics** - Request rates, error rates, latency percentiles (RED metrics)

## How We Achieve It

| Capability | Solution |
|------------|----------|
| Tracing | [OpenTelemetry Java Agent](https://opentelemetry.io/docs/zero-code/java/agent/) auto-instruments the app (Spring MVC, JDBC, Redis, HTTP clients) |
| Log Correlation | OTEL agent injects `trace_id`/`span_id` into SLF4J MDC; Logback includes them in output |
| Metrics | Generated from traces by the backend (Tempo in local stack, or your vendor) |
| Trace Storage | Tempo (local) or your vendor (Datadog, Honeycomb, Grafana Cloud, etc.) |
| Log Storage | Loki (local) or your vendor |
| Metrics Storage | Prometheus (local) or your vendor |
| Visualization | Grafana (local) or your vendor |

## High-Level Implementation

### Java Agent Integration

- **Version:** Single source of truth in `gradle.properties` → `opentelemetryJavaagentVersion`
- **bootRun:** Gradle downloads the agent as a dependency (`otelAgent` configuration in `backend/app/build.gradle`)
- **Docker:** The Dockerfile downloads the agent via `wget` during image build, with version passed from Gradle (`docker/app/Dockerfile`)
- **Attachment:** JVM flag `-javaagent:path/to/agent.jar` attaches it at startup

### Two Ways to Run with Tracing

| Method | Use Case | How |
|--------|----------|-----|
| `./gradlew bootRun` | Local development, quick iteration | `OTEL_JAVAAGENT_ENABLED=true ./gradlew bootRun` |
| Docker | Testing the full stack, CI, production-like | Set `OTEL_JAVAAGENT_ENABLED=true` in docker-compose environment (already done in `docker-compose.local-observability-stack.yaml`) |

Both methods use the same agent version from `gradle.properties`.

### Local Observability Stack

For local development, we provide a complete observability backend in `docker/local-observability-stack/`:

```
Tolgee App
    │
    ├─── traces (OTLP) ──→ OTEL Collector ──→ Tempo ──→ Prometheus
    │                                           │
    └─── logs (stdout) ──→ Promtail ──→ Loki ───┘
                                                │
                                                v
                                            Grafana
```

**Start it:**
```bash
cd docker && docker compose -f docker-compose.local-observability-stack.yaml up -d
```

**Access Grafana:** http://localhost:3000

For a hands-on tutorial, see `docs/local-observability-stack/`.

## What's Production vs Development-Only

| What | Scope | Notes |
|------|-------|-------|
| OpenTelemetry Java Agent | **Prod + Dev** | Same agent, different endpoint target |
| Logback MDC fields | **Prod + Dev** | Trace context in every log line |
| `gradle.properties` version | **Prod + Dev** | Single source of truth for agent version |
| `docker/local-observability-stack/` | **Dev only** | Local Grafana/Tempo/Loki/Prometheus |

"Prod + Dev" = used everywhere. The only difference is where `OTEL_EXPORTER_OTLP_ENDPOINT` points: your vendor in production, the local collector in development.

## Implementation Details

These details apply to the application itself, regardless of environment.

### Agent Configuration

Environment variables control the agent:
- `OTEL_JAVAAGENT_ENABLED=true` - attach agent to JVM
- `OTEL_EXPORTER_OTLP_ENDPOINT` - where to send traces (e.g., `http://otel-collector:4317`)
- `OTEL_SERVICE_NAME` - identifies this service in traces

### What Gets Instrumented

The agent auto-instruments without code changes:
- HTTP server requests (Spring MVC)
- HTTP client calls (RestTemplate, WebClient)
- Database queries (JDBC, Hibernate)
- Cache operations (Redis)
- ...and more

## Implementation Details: Local Stack Only

These details are specific to `docker/local-observability-stack/` and don't apply to production.

### Sampling

The OTEL Collector (`otel-collector-config.yaml`) implements tail-based sampling:
- Collects all spans initially
- Makes sampling decision after seeing complete trace
- Can prioritize error traces and slow traces

### Span Metrics

Tempo's `metrics_generator` produces RED metrics from traces:
- `traces_spanmetrics_calls_total` - request counts
- `traces_spanmetrics_latency_bucket` - latency histograms

Prometheus scrapes these from Tempo.

### Configuration Files

All in `docker/local-observability-stack/`:

| File | Purpose |
|------|---------|
| `otel-collector-config.yaml` | Receive traces, apply sampling |
| `tempo-config.yaml` | Store traces, generate metrics |
| `loki-config.yaml` | Store logs |
| `promtail-config.yaml` | Scrape container logs, extract trace_id |
| `prometheus.yaml` | Scrape span metrics from Tempo |
| `grafana/datasources.yaml` | Connect Grafana to all backends |
