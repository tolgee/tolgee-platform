# Part 1: Setting Up the Tracing Stack

**What you'll learn:** How to start the tracing infrastructure and verify it's working.

*Skip if: You already have Grafana running at http://localhost:3000 with Tempo traces visible.*

---

## Choose Your Setup

There are two ways to run Tolgee with the observability stack:

|                   | Option A: Full Docker Stack                      | Option B: Local Tolgee + Docker Observability            |
|-------------------|--------------------------------------------------|----------------------------------------------------------|
| **Best for**      | Quick setup, testing production-like environment | Active development, debugging with IDE, faster iteration |
| **Tolgee runs**   | In Docker                                        | Locally via Gradle                                       |
| **Observability** | In Docker                                        | In Docker                                                |

Pick the option that fits your workflow and follow the corresponding section below.

---

## 1.1 Option A: Full Docker Stack

This runs everything in Docker, including Tolgee with OpenTelemetry already configured.

**Build the Docker image:**

```bash
./gradlew docker
```

**Start the stack** from the `docker` directory:

```bash
cd docker && docker compose -f docker-compose.local-observability-stack.yaml up -d
```

This starts:
- **Tolgee** (port 8080) - the application, with OpenTelemetry agent attached
- **Grafana** (port 3000) - UI for viewing traces and logs
- **Tempo** - stores and queries traces
- **Loki** - stores and queries logs
- **Promtail** - ships logs from containers to Loki
- **Prometheus** - stores metrics generated from traces
- **OpenTelemetry Collector** - receives traces and forwards them
- **PostgreSQL** and **Redis** - Tolgee dependencies

**Verify all services are healthy:**

```bash
docker compose -f docker-compose.local-observability-stack.yaml ps
```

All services should show `Up` or `healthy`.

---

## 1.2 Option B: Local Tolgee with Docker Observability

This runs Tolgee locally via Gradle while observability services and dependencies run in Docker. Useful for development when you want to debug Tolgee in your IDE.

**Start the observability stack** from the `docker` directory:

```bash
cd docker && docker compose -f docker-compose.local-observability-stack-dev.yaml up -d
```

This starts:
- **Grafana** (port 3000) - UI for viewing traces and logs
- **Tempo** - stores and queries traces
- **Loki** - stores and queries logs
- **Promtail** - ships logs from containers to Loki
- **Prometheus** - stores metrics generated from traces
- **OpenTelemetry Collector** - receives traces and forwards them
- **PostgreSQL** and **Redis** - Tolgee dependencies

**Run Tolgee locally with tracing enabled:**

```bash
./gradlew server-app:bootRunWithObservability --args='--spring.profiles.active=dev'
```

**Verify all services are healthy:**

```bash
docker compose -f docker-compose.local-observability-stack-dev.yaml ps
```

All services should show `Up` or `healthy`.

---

## 1.3 Access Grafana

Open http://localhost:3000 in your browser.

No login required - anonymous access is enabled for local development.

**Verify Tempo datasource:**

1. Click the hamburger menu (☰) to open the navigation:

![Click the hamburger menu to open navigation](images/1-1-grafana-menu-button.png)

2. Click **Explore** in the menu:

![Click Explore in the menu](images/1-2-grafana-menu-explore.png)

3. In the datasource dropdown (top left), select **Tempo**. You should see the TraceQL query interface (don't enter any query just yet):

![Grafana Explore with Tempo datasource selected](images/1-3-grafana-explore-tempo.png)

If you see "Datasource not found", the infrastructure may still be starting. Wait 30 seconds and refresh.

## 1.4 Verify Traces Are Generated

Tolgee should now be running with tracing enabled (either in Docker or locally, depending on your chosen option). Let's verify traces are being collected.

1. Make a request to Tolgee, e.g.:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. In Grafana Explore (with Tempo selected), run this query:
   ```traceql
   {resource.service.name="tolgee-platform"}
   ```

3. You should see traces appear in the results (the specific traces you see may be different though):

![Trace results showing Tolgee requests](images/1-4-grafana-trace-results.png)

If no traces appear, check the following (use the compose file matching your chosen option):

- Check that services are healthy: `docker compose -f <your-compose-file>.yaml ps`
- Check Tolgee logs for OpenTelemetry messages (Option A only): `docker compose -f docker-compose.local-observability-stack.yaml logs app`
- Verify the OTel Collector is running: `docker compose -f <your-compose-file>.yaml logs otel-collector`
- Wait 10-15 seconds for traces to be ingested

---

**Checkpoint:** You should now have:
- All services running (including Tolgee with tracing)
- Grafana accessible at http://localhost:3000
- Tempo showing traces from Tolgee requests

**Next:** [Part 2: Understanding Traces and Spans](2_Understanding_Traces_and_Spans.md)
