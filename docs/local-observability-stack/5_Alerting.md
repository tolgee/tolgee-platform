# Part 5: Alerting

**Goal:** Set up proactive notifications when performance degrades - before users complain.

Alerting requires *continuous* metric evaluation - something running in the background to check thresholds and trigger notifications. The span metrics from Part 4 are perfect for this because they're always available in Prometheus.

---

## 5.1 How Alerting Works

### The Flow

```text
Traces -> Tempo -> Metrics Generator -> Prometheus -> Grafana Alerting -> Notifications
```

1. **Spans arrive at Tempo** - your application sends trace data
2. **Metrics Generator computes RED metrics** - Rate, Errors, Duration
3. **Metrics written to Prometheus** - stored as time series
4. **Grafana evaluates alert rules** - periodically checks conditions
5. **Alertmanager sends notifications** - when conditions are met

### Available Metrics for Alerting

These metrics are automatically generated from traces:

| Metric | Type | Use Case |
|--------|------|----------|
| `traces_spanmetrics_latency_bucket` | histogram | Percentile latency alerts (p95, p99) |
| `traces_spanmetrics_calls_total` | counter | Request rate, error rate alerts |
| `traces_spanmetrics_size_total` | counter | Throughput alerts |

Query these via the **Prometheus** datasource in Grafana.

---

## 5.2 Setting Up Contact Points

A contact point defines *where* notifications go when an alert fires.

### Creating a Contact Point

Open the menu and navigate to **Alerting**:

![Menu with Alerting option](images/4-9-1-menu-alerting.png)

You'll see the Alerting overview page:

![Alerting overview page](images/4-9-2-alerting-overview.png)

Click **Manage contact points** to see existing contact points:

![Contact points list](images/4-9-3-contact-points-page.png)

Click **Create contact point** to open the form:

![Create contact point form](images/4-9-4-contact-point-form.png)

Enter a name for your contact point (e.g., "test-webhook"):

![Contact point name entered](images/4-9-5-contact-point-name.png)

### Choosing an Integration

Click the **Integration** dropdown to see available options:

![Integration dropdown with options](images/4-9-6-integration-dropdown.png)

Common integrations:
- **Slack** - team channel notifications
- **PagerDuty** - on-call alerting with escalation
- **Email** - simple email notifications
- **Webhook** - custom integrations (Discord, Teams, custom systems)

### Configuring a Webhook

Select **Webhook** for testing:

![Webhook integration selected](images/4-9-7-webhook-selected.png)

**For local testing:** Use the provided webhook server:
```bash
python3 docs/local-observability-stack/test-webhook-server.py
```

Enter the webhook URL: `http://host.docker.internal:9999/webhook`

![Webhook URL configured](images/4-9-8-webhook-configured.png)

### Testing Contact Points

Click **Test** to verify your configuration:

![Test contact point dialog](images/4-9-9-test-dialog.png)

> **Note:** If you don't have a webhook server running, the test will fail with a connection error - this is expected:
>
> ![Test error - connection refused](images/4-9-10-test-error.png)

Click **Save contact point** to save:

![Contact point saved](images/4-9-11-contact-point-saved.png)

---

## 5.3 Creating Alert Rules

An alert rule defines *what condition* to check and *how often*.

### Opening the Alert Rules Page

From the Alerting menu, click **Alert rules**:

![Alerting submenu with Alert rules](images/4-9-12-alert-rules-menu.png)

You'll see the Alert rules page (empty if you haven't created any rules yet):

![Alert rules page](images/4-9-13-alert-rules-page.png)

### Creating a New Alert Rule

Click **New alert rule** to open the form:

![New alert rule form](images/4-9-14-new-alert-rule-form.png)

#### Step 1: Name the Alert

Enter a descriptive name (e.g., `endpoint-latency-high`):

![Alert name entered](images/4-9-15-alert-name-entered.png)

#### Step 2: Configure the Query

Click the datasource dropdown and select **Prometheus**:

![Datasource dropdown](images/4-9-16-datasource-dropdown.png)

![Prometheus datasource selected](images/4-9-17-prometheus-selected.png)

Enter the PromQL query:
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket[5m])) by (le, span_name))
```

This calculates p95 latency across all endpoints.

![Query entered](images/4-9-18-query-entered.png)

#### Step 3: Set the Threshold

Configure the condition:
- Reduce: **Last** value
- Threshold: **IS ABOVE 0.5** (500ms)

![Threshold configured](images/4-9-19-threshold-configured.png)

#### Step 4: Configure Evaluation

Click **New folder** to create a folder for your alerts:

![New folder dialog](images/4-9-20-new-folder-dialog.png)

After creating the folder (e.g., `performance-alerts`):

![Folder created](images/4-9-21-folder-created.png)

Click **New evaluation group** to set the evaluation interval:

![New evaluation group dialog](images/4-9-22-new-evaluation-group-dialog.png)

Configure the evaluation group (e.g., `latency-checks` with `1m` interval):

![Evaluation behavior configured](images/4-9-23-evaluation-configured.png)

**Evaluation groups** control how often rules are checked. All rules in the same group evaluate together. Common intervals:
- `1m` - near real-time alerting
- `5m` - balanced approach
- `15m` - less urgent checks

#### Step 5: Select Contact Point

Click the contact point dropdown:

![Contact point dropdown](images/4-9-24-contact-point-dropdown.png)

Select your contact point (e.g., `test-webhook`):

![Contact point selected](images/4-9-25-contact-point-selected.png)

#### Step 6: Save the Rule

Click **Save rule and exit**:

![Rule saved successfully](images/4-9-26-rule-saved-success.png)

The rule now appears in the Alert rules list:

![Alert rule in list](images/4-9-27-alert-rule-in-list.png)

### Testing Your Alert

Generate traffic to Tolgee endpoints. If any endpoint's p95 latency exceeds 500ms for the pending period, you'll receive an alert notification.

---

## 5.4 Alert Expression Reference

These PromQL expressions can be used in Grafana alert rules. Use the **Prometheus** datasource.

### Latency Alerts

**Endpoint p95 latency > 1 second:**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name="/v2/projects/{projectId}"}[5m])) by (le)) > 1
```

**Endpoint p99 latency > 2 seconds:**
```promql
histogram_quantile(0.99, sum(rate(traces_spanmetrics_latency_bucket{span_name="/v2/projects/{projectId}"}[5m])) by (le)) > 2
```

**Any endpoint p95 > 500ms (alert per endpoint):**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket[5m])) by (le, span_name)) > 0.5
```

### Error Rate Alerts

**Endpoint error rate > 5%:**
```promql
sum(rate(traces_spanmetrics_calls_total{span_name="/v2/projects/{projectId}", status_code="STATUS_CODE_ERROR"}[5m]))
/
sum(rate(traces_spanmetrics_calls_total{span_name="/v2/projects/{projectId}"}[5m]))
> 0.05
```

**Any endpoint error rate > 1%:**
```promql
sum(rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])) by (span_name)
/
sum(rate(traces_spanmetrics_calls_total[5m])) by (span_name)
> 0.01
```

### Database Alerts

**Database query p95 > 100ms:**
```promql
histogram_quantile(0.95, sum(rate(traces_spanmetrics_latency_bucket{span_name=~"SELECT.*|INSERT.*|UPDATE.*|DELETE.*"}[5m])) by (le, span_name)) > 0.1
```

**Database error rate > 1%:**
```promql
sum(rate(traces_spanmetrics_calls_total{span_name=~"SELECT.*|INSERT.*|UPDATE.*|DELETE.*", status_code="STATUS_CODE_ERROR"}[5m])) by (span_name)
/
sum(rate(traces_spanmetrics_calls_total{span_name=~"SELECT.*|INSERT.*|UPDATE.*|DELETE.*"}[5m])) by (span_name)
> 0.01
```

### Tips for Effective Alerts

**Avoid alert fatigue:**
- Set thresholds high enough to avoid constant firing
- Use `for` duration to require sustained problems (e.g., "above threshold for 5 minutes")
- Start conservative and tighten thresholds over time

**Make alerts actionable:**
- Include `span_name` in your query's `by` clause so the alert tells you *which* endpoint
- Link to relevant dashboards in alert annotations
- Include runbook links for common issues

**Test before relying on alerts:**
- Verify the query returns expected results in Explore first
- Test contact points receive notifications
- Trigger test alerts by temporarily lowering thresholds

*Deeper dive: [Grafana Alerting](https://grafana.com/docs/grafana/latest/alerting/)*

---

**Checkpoint:** You now know how to:
- Set up notification channels (contact points)
- Create alert rules with PromQL queries
- Configure evaluation intervals
- Write expressions for latency, error rate, and database alerts

**Next:** [Part 6: Practical Reference Guide](6_Practical_Reference.md)
