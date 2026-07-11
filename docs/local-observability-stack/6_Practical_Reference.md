# Part 6: Practical Reference Guide

**Goal:** A comprehensive "when to do what & how" reference for everyday debugging.

Bookmark this page. It combines everything you've learned into practical, scenario-based guidance.

---

## 6.1 When to Use Which Tool

### The Three Pillars

| Pillar | Answers | Retention | Cost |
|--------|---------|-----------|------|
| **Logs** | What happened? What were the values? | Days-weeks | Medium |
| **Metrics** | How much? How often? Trends? | Months-years | Low |
| **Traces** | Where did time go? How did operations connect? | Hours-days | High |

### Decision Flowchart

```text
"Something is wrong with my application"
           |
           v
+------------------------+
| Do you need aggregate  |     YES
| statistics over time?  |-----------> METRICS
| (rates, percentiles,   |            - Prometheus/PromQL
| trends, counts)        |            - Span metrics
+------------------------+            - TraceQL metrics
           | NO
           v
+------------------------+
| Do you need to see     |     YES
| how a single request   |-----------> TRACES
| flowed through the     |            - Tempo/TraceQL
| system?                |            - Waterfall view
+------------------------+
           | NO
           v
+------------------------+
| Do you need exact      |     YES
| values, full error     |-----------> LOGS
| messages, or debug     |            - Loki/LogQL
| output?                |            - Application logs
+------------------------+
```

### The Typical Workflow

1. **Metrics** alert you something is wrong (latency spike, error rate increase)
2. **Traces** show you where the problem is (which operation, which dependency)
3. **Logs** show you the details (exact error, input values, debug output)

### What Tracing is NOT Good For

| Use Case | Problem with Tracing | Better Tool |
|----------|---------------------|-------------|
| Aggregate statistics | Traces show individual requests, not distributions | Metrics |
| Long-term trending | Traces retained for hours/days, not months | Metrics |
| Counting things | Traces don't aggregate counts | Metrics |
| Exact values at a point in time | Attributes may be truncated | Logs |
| High-cardinality debugging | Trace storage can explode | Logs with sampling |

*Deeper dive: [IBM: Observability Pillars](https://www.ibm.com/think/insights/observability-pillars)*

---

## 6.2 Common Span Attributes

### HTTP Attributes

| Attribute | Description | Example |
|-----------|-------------|---------|
| `http.request.method` | HTTP method | `GET`, `POST` |
| `http.route` | Path template | `/v2/projects/{projectId}` |
| `http.response.status_code` | Response status | `200`, `404`, `500` |
| `url.path` | Full request path | `/v2/projects/123` |
| `http.request.header.*` | Request headers | `http.request.header.authorization` |
| `http.response.header.*` | Response headers | `http.response.header.content-type` |

### Database Attributes

| Attribute | Description | Example |
|-----------|-------------|---------|
| `db.system` | Database type | `postgresql` |
| `db.operation` | SQL operation | `SELECT`, `INSERT` |
| `db.statement` | Query text | `SELECT * FROM...` |
| `db.name` | Database name | `tolgee` |

### Resource Attributes

| Attribute | Description | Example |
|-----------|-------------|---------|
| `service.name` | Service identifier | `tolgee-platform` |
| `service.version` | Version string | `1.0.0` |
| `host.name` | Hostname | `server-1` |

### Querying Nested Attributes

TraceQL supports dot notation:
```traceql
{span.http.request.header.authorization != ""}
```

---

## 6.3 Filtering Techniques

### HTTP Requests Only

By default, Tempo returns all traces including internal/scheduled operations. To focus on HTTP requests:

```traceql
{span.http.request.method=~"GET|POST|PUT|DELETE|PATCH"}
```

Or filter by route pattern:
```traceql
{span.http.route=~"/v2/.*"}
```

### Finding Root Spans

Root spans (no parent) typically represent incoming requests:
```traceql
{span.url.path != "" && resource.service.name="tolgee-platform"}
```

### Excluding Health Checks

```traceql
{span.http.route!~".*/health.*" && span.http.route!~".*/actuator.*"}
```

### Regex Patterns

Match patterns:
```traceql
{span.http.route=~"/v2/projects/.*"}
```

Match any value exists:
```traceql
{span.db.statement != ""}
```

### Combining Conditions

```traceql
{span.http.request.method="GET" && duration > 100ms}
{span.http.response.status_code=404 || span.http.response.status_code=500}
{span.http.route=~"/v2/projects.*" && status=error}
```

---

## 6.4 Debugging Scenarios

Each scenario follows: **Symptom -> Tool(s) -> Query/Approach -> What to Look For**

### Basic Scenarios (Trace-Focused)

#### "Why is this endpoint slow?"

**Symptom:** A user reports a page takes 5+ seconds to load. Logs show the request completed, but don't explain *where* the time went.

**Tool:** Traces - the waterfall view immediately reveals which operation consumed the time.

**Query:**
```traceql
{span.http.route="/v2/projects/{projectId}" && duration > 1s}
```

**What to look for:**
- **One span taking most of the time** -> single slow operation (slow query, slow external call)
- **Many sequential spans** -> unnecessary sequential operations (could parallelize?)
- **Gaps between spans** -> time spent in uninstrumented application code

---

#### "Is this an N+1 query problem?"

**Symptom:** An endpoint is slow, and you suspect the ORM might be making too many database queries.

**Tool:** Traces - N+1 problems are visually obvious as dozens of nearly-identical query spans.

**Query:**
```traceql
{span.http.route="/v2/projects" && span.db.system="postgresql"}
```

**What to look for:**
- **Many SELECT spans with similar structure** -> N+1 problem
- **Pattern:** 1 query for list + N queries for related entities
- **Fix indicator:** after optimization, should see 1-2 queries instead of N+1

**Visual pattern:**
```text
GET /v2/projects                              [=====================================]
  SELECT ... FROM project                     [==]
  SELECT ... FROM language WHERE project = ?      [=]
  SELECT ... FROM language WHERE project = ?         [=]
  SELECT ... FROM language WHERE project = ?            [=]
  ... (many more identical queries)
```

---

#### "What's causing intermittent errors?"

**Symptom:** Users report occasional failures, but the error rate is low and hard to reproduce.

**Tool:** Traces - search for traces containing errors and see the full request context.

**Query:**
```traceql
{status=error}
```

Or by HTTP status:
```traceql
{span.http.response.status_code >= 500}
```

**What to look for:**
- **Which span has the error?** Database operation, external call, or application code?
- **Error attributes:** `exception.type`, `exception.message`, `exception.stacktrace`
- **What happened before the error?** Preceding spans give context
- **Is there a pattern?** Always after a certain operation, always with certain attributes?

---

#### "Which requests are affected by this slow database?"

**Symptom:** The database is under load. You want to know which API endpoints are making the most/heaviest database calls.

**Tool:** Traces - find all slow database spans and see which requests triggered them.

**Query:**
```traceql
{span.db.system="postgresql" && duration > 100ms}
```

**What to look for:**
- Which endpoints generate these slow queries?
- Are certain query patterns slower than others?
- Check `db.statement`, `db.operation`, and parent span (which endpoint triggered this?)

**For high query volume issues** (many fast queries causing load), use span metrics:
```promql
sum(rate(traces_spanmetrics_calls_total{span_name=~"SELECT.*|INSERT.*|UPDATE.*|DELETE.*"}[5m])) by (span_name)
```

---

#### "How does this request flow through the system?"

**Symptom:** You're new to the codebase or debugging an unfamiliar feature.

**Tool:** Traces - living documentation of request flow.

**Approach:**
1. Perform the action in the UI (or make the API call)
2. Find the resulting trace
3. Read through the spans chronologically

**Query:**
```traceql
{span.http.request.method="POST" && span.http.route="/v2/projects"}
```

**What to look for:**
- Sequence of operations (authentication, validation, business logic, persistence)
- Which external services are called
- Which database tables are touched
- Unexpected operations (why is this calling *that*?)

---

#### "Why did this request time out?"

**Symptom:** A request timed out. Logs show a timeout error, but not *where* the time was spent.

**Tool:** Traces - even incomplete traces show what happened before the timeout.

**Query:**
```traceql
{span.http.route="/v2/projects/{projectId}" && duration > 30s}
```

Or look for incomplete traces:
```traceql
{status=error && span.http.route="/v2/projects/{projectId}"}
```

**What to look for:**
- **The last span** - often indicates where things got stuck
- **The longest span** - what operation took too long
- **Missing expected spans** - the timeout may have prevented later operations
- **External call spans** - was it waiting for an external service?

---

### Metrics-Aware Scenarios

These scenarios combine traces with metrics for deeper insights.

#### "Has this endpoint gotten slower over the past week?"

**Symptom:** Users mention the app "feels slower" but you're not sure if it's real.

**Tool:** Span metrics dashboard

**Query (PromQL):**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="GET /v2/projects"}[5m])) by (le))
```

**Approach:**
1. Create a dashboard with the p95 latency for the endpoint
2. Set the time range to 7 days (or longer)
3. Look for upward trends or step changes that correlate with deployments

**What to look for:**
- **Gradual increase** -> growing data volume, accumulated technical debt
- **Step change at a specific time** -> correlate with deployment history
- **Spiky pattern** -> periodic load (batch jobs, scheduled tasks)

---

#### "What's the error rate trend for this service?"

**Symptom:** You want to understand if error rates are stable or worsening.

**Tools:** TraceQL metrics for quick exploration, PromQL for dashboards

**TraceQL query (ad-hoc):**
```traceql
{ span.http.route =~ "/v2/.*" && status = error } | rate()
```

**PromQL query (dashboard):**
```promql
sum(rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])) by (span_name)
/
sum(rate(traces_spanmetrics_calls_total[5m])) by (span_name)
```

**What to look for:**
- Error rate percentage over time
- Which endpoints have the highest error rates
- Correlation with deployments or infrastructure changes

---

#### "Did my optimization actually help?"

**Symptom:** You've deployed a performance fix and want to verify it worked.

**Tools:** Span metrics for before/after comparison, traces for understanding changes

**Approach:**
1. Note the deployment timestamp
2. Query p95 latency for the affected endpoint, time range spanning before and after
3. Compare individual traces from before and after to see what changed

**PromQL query:**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="GET /v2/projects/{projectId}"}[5m])) by (le))
```

**What to look for:**
- **Step down at deployment time** -> optimization worked
- **No change** -> fix didn't address the bottleneck, or bottleneck is elsewhere
- **Worse performance** -> regression, roll back and investigate

**For deeper analysis:** Compare traces from before and after:
1. Find a trace from before (use time picker)
2. Find a trace from after
3. Open waterfalls side-by-side and compare span counts, durations

---

#### "Which endpoints need performance work?"

**Symptom:** You have time for optimization but need to prioritize.

**Tool:** Dashboard with latency heatmap across all endpoints

**PromQL query:**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name=~"GET.*|POST.*|PUT.*|DELETE.*"}[5m])) by (le, span_name))
```

**What to look for:**
- **Highest absolute latency** -> user-facing impact
- **Highest latency relative to complexity** -> quick wins
- **Most frequently called + high latency** -> highest total time savings
- **Error rate alongside latency** -> endpoints that are both slow and broken

---

#### "Are database queries the bottleneck?"

**Symptom:** Application is slow but you're not sure if it's DB, external APIs, or application code.

**Tool:** Dashboard comparing time spent in different categories

**PromQL queries:**
```promql
# Total DB time per second
sum(rate(traces_spanmetrics_latency_sum{span_name=~"SELECT.*|INSERT.*|UPDATE.*|DELETE.*"}[5m]))

# Total HTTP endpoint time per second
sum(rate(traces_spanmetrics_latency_sum{span_name=~"GET.*|POST.*|PUT.*|DELETE.*"}[5m]))
```

**What to look for:**
- If DB latency sum is close to HTTP latency sum -> DB is the bottleneck
- Large gap between them -> time is spent in application code
- Individual traces confirm where time goes

---

### Alerting Scenarios

These scenarios help you set up proactive monitoring.

#### "Alert me before users notice slowdowns"

**Goal:** Catch performance degradation early.

**Alert expression (p95 latency > 1s):**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="GET /v2/projects"}[5m])) by (le)) > 1
```

**Tips:**
- Use p95 (not average) to catch tail latency
- Set threshold based on your SLO (e.g., "95% of requests under 1 second")
- Add `for: 5m` to avoid alerting on brief spikes

---

#### "Catch error spikes early"

**Goal:** Know when error rates exceed normal levels.

**Alert expression (error rate > 5%):**
```promql
sum(rate(traces_spanmetrics_calls_total{span_name="GET /v2/projects", status_code="STATUS_CODE_ERROR"}[5m]))
/
sum(rate(traces_spanmetrics_calls_total{span_name="GET /v2/projects"}[5m]))
> 0.05
```

**Tips:**
- Baseline your normal error rate first
- Consider different thresholds for different endpoints (auth endpoints may have higher failure rates)
- Alert on rate changes, not just absolute thresholds

---

#### "Monitor database health"

**Goal:** Detect database issues before they cascade.

**Alert expressions:**

**Slow queries (p95 > 100ms):**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name=~"SELECT.*|INSERT.*|UPDATE.*|DELETE.*"}[5m])) by (le, span_name)) > 0.1
```

**DB errors:**
```promql
sum(rate(traces_spanmetrics_calls_total{span_name=~"SELECT.*|INSERT.*|UPDATE.*|DELETE.*", status_code="STATUS_CODE_ERROR"}[5m])) by (span_name) > 0
```

**Tips:**
- Correlate DB alerts with connection pool metrics if available
- High query count + normal latency can still indicate a problem (connection exhaustion)

---

#### "Track deployment impact"

**Goal:** Know quickly if a deployment causes problems.

**Approach:** Combine alerts with dashboards

1. **Create alerts for key SLOs** (latency, error rate)
2. **Annotate deployments on dashboards** (many CI/CD systems can push Grafana annotations)
3. **Review dashboards after each deployment**

**Alert expression (sudden latency increase):**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="GET /v2/projects"}[5m])) by (le))
>
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="GET /v2/projects"}[5m] offset 1h)) by (le)) * 1.5
```

This alerts when current latency is 50% higher than an hour ago.

---

### Combined Scenarios

These scenarios show how to use all the tools together.

#### "Debug a production incident end-to-end"

**Symptom:** Alert fires: "p95 latency > 2s for GET /v2/projects"

**Workflow:**

1. **Check dashboards first**
   - Is this a gradual increase or sudden spike?
   - Did it correlate with a deployment?
   - Which specific endpoint(s) are affected?

2. **Find representative traces**
   ```traceql
   {span.http.route="/v2/projects" && duration > 2s}
   ```

3. **Analyze the waterfall**
   - Which span is taking the time?
   - Is it a single slow operation or accumulated latency?

4. **Correlate with logs** (click "Logs for this span")
   - What's the exact error message?
   - What were the input values?

5. **Check if it's widespread**

   Use TraceQL metrics to see what percentage of requests exceed a threshold:
   ```traceql
   { span.http.route = "/v2/projects" && duration > 2s } | rate()
   ```
   Compare this to the overall request rate to understand the scope of impact.

---

#### "Set up monitoring for a new feature"

**Scenario:** You're launching a new API endpoint and want to monitor it properly.

**Workflow:**

1. **Identify the key spans** (endpoint name, any new DB queries, external calls)

2. **Decide on metrics dimensions**
   - Will span_name alone be enough?
   - Do you need to split by user type, region, etc.? (Requires Tempo config)

3. **Create a dashboard**
   - P95 latency over time
   - Request rate
   - Error rate
   - Breakdown by status code

4. **Set up alerts**
   - P95 > your SLO threshold
   - Error rate > baseline (probably start generous and tighten)

5. **After launch, review**
   - Are metrics showing expected patterns?
   - Sample some traces to verify instrumentation is correct
   - Adjust alert thresholds based on real behavior

---

#### "Investigate a customer complaint"

**Scenario:** A customer says "the app was slow yesterday around 3pm"

**Workflow:**

1. **Find their trace** (if you have a way to identify their request)
   - By user ID if it's in span attributes
   - By time window + endpoint they were using
   ```traceql
   {span.http.route="/v2/projects" && span.user.id="customer-123"}
   ```
   (Note: `user.id` requires custom instrumentation)

2. **If you can't identify their specific trace, check metrics**
   - Was there a general slowdown around that time?
   ```promql
   histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="GET /v2/projects"}[5m])) by (le))
   ```
   - Set time picker to the time they mentioned

3. **Find traces from that time window**
   ```traceql
   {span.http.route="/v2/projects" && duration > 1s}
   ```
   (with time picker set to their reported time)

4. **Look for patterns**
   - Was it a specific operation that was slow?
   - Were multiple users affected or just this one?
   - Any correlation with system events (deployments, DB maintenance)?

---

## 6.5 Quick Reference

### TraceQL Syntax

```traceql
# Basic filters
{resource.service.name="tolgee-platform"}
{span.http.route="/v2/projects/{projectId}"}
{duration > 500ms}
{status=error}

# Combining conditions
{span.http.request.method="GET" && duration > 100ms}
{span.http.response.status_code=404 || span.http.response.status_code=500}

# Regex
{span.http.route=~"/v2/projects.*"}
{span.http.route!~"/health.*"}

# Database
{span.db.system="postgresql"}
{span.db.system="postgresql" && duration > 50ms}

# Metrics (append to any selector)
{span.http.route="/v2/projects"} | rate()
{span.http.route="/v2/projects"} | quantile_over_time(duration, 0.95)
{status=error} | rate() by (span.http.route)
```

### PromQL for Span Metrics

```promql
# P95 latency for all endpoints
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket[5m])) by (le, span_name))

# P95 latency for specific endpoint
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="GET /v2/projects"}[5m])) by (le))

# Request rate by endpoint
sum(rate(traces_spanmetrics_calls_total[5m])) by (span_name)

# Error rate for specific endpoint
sum(rate(traces_spanmetrics_calls_total{span_name="GET /v2/projects", status_code="STATUS_CODE_ERROR"}[5m]))
/
sum(rate(traces_spanmetrics_calls_total{span_name="GET /v2/projects"}[5m]))

# Average latency
rate(traces_spanmetrics_latency_sum{span_name="GET /v2/projects"}[5m])
/
rate(traces_spanmetrics_latency_count{span_name="GET /v2/projects"}[5m])
```

### Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `OTEL_JAVAAGENT_ENABLED` | Enable OpenTelemetry agent | `true` |
| `OTEL_SERVICE_NAME` | Service name in traces | `tolgee-platform` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Collector endpoint | `http://localhost:4318` |
| `OTEL_INSTRUMENTATION_LOGBACK_MDC_ENABLED` | Add trace IDs to logs | `true` |

### External Documentation

- [OpenTelemetry Traces](https://opentelemetry.io/docs/concepts/signals/traces/) - official concepts
- [TraceQL Reference](https://grafana.com/docs/tempo/latest/traceql/) - query language
- [TraceQL Metrics](https://grafana.com/docs/tempo/latest/metrics-from-traces/metrics-queries/) - computing metrics from traces
- [Span Metrics Generator](https://grafana.com/docs/tempo/latest/metrics-from-traces/span-metrics/span-metrics-metrics-generator/) - continuous metrics
- [Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/) - alerting system
- [Prometheus PromQL](https://prometheus.io/docs/prometheus/latest/querying/basics/) - query language

---

## 6.6 Conclusion

You now know how to:

1. **Set up** the tracing stack (Part 1)
2. **Understand** traces and spans (Part 2)
3. **Navigate** Grafana Tempo with TraceQL (Part 3)
4. **Derive metrics** from traces for trends and dashboards (Part 4)
5. **Set up alerts** for proactive monitoring (Part 5)
6. **Debug** real-world problems using traces, metrics, and logs together (Part 6)

### What to Do Next

- **Bookmark this guide** - use it as a reference when debugging
- **Experiment** - run queries against real Tolgee requests
- **Build a dashboard** - track the endpoints you care about most
- **Set up alerts** - start with loose thresholds, tighten over time
- **Practice the workflow** - metrics -> traces -> logs -> fix

### Getting Help

- **TraceQL syntax:** [Grafana Tempo Docs](https://grafana.com/docs/tempo/latest/traceql/)
- **Tracing concepts:** [OpenTelemetry Docs](https://opentelemetry.io/docs/concepts/signals/traces/)
- **Tolgee tracing config:** See comments in `docker/docker-compose.local-observability-stack.yaml`

---

**Return to:** [Start](0_START_HERE.md)
