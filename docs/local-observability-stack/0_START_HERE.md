# Distributed Tracing Tutorial for Tolgee

A hands-on guide to debugging and understanding your application using distributed tracing.

## What is Distributed Tracing?

Distributed tracing tracks requests as they flow through your system. Each request generates a **trace** - a tree of **spans** showing every operation: HTTP calls, database queries, internal method calls.

Unlike logs (which tell you *that* something happened) or metrics (which tell you *how often* and *how much*), traces tell you **where time went** and **how operations connect**.

*For more background: [A Beginner's Guide to Distributed Tracing](https://grafana.com/blog/a-beginners-guide-to-distributed-tracing-and-how-it-can-increase-an-applications-performance/) or search `three pillars of observability`*

## Why Use Tracing?

Reach for tracing when you need to answer:
- "Why is this endpoint slow?" - see exactly which operation took the time
- "Is this an N+1 query problem?" - see every database call in one view
- "What actually happens when a user does X?" - trace the full request flow
- "Where did this error come from?" - see the full context leading to failure

## Prerequisites

Before starting, you need:
- Docker and Docker Compose installed
- Tolgee repository cloned
- Basic familiarity with HTTP APIs

**No prior tracing experience required.** This tutorial assumes you've never used tracing before.

## Tutorial Parts

| Part | Topic | Time |
|------|-------|------|
| [1. Setting Up the Tracing Stack](1_Setting_Up_the_Tracing_Stack.md) | Start infrastructure and verify it works | 5 min |
| [2. Understanding Traces and Spans](2_Understanding_Traces_and_Spans.md) | Core concepts: traces, spans, attributes | 10 min |
| [3. Navigating Grafana Tempo](3_Navigating_Grafana_Tempo.md) | Search traces, read waterfalls, correlate logs | 15 min |
| [4. Metrics from Traces](4_Metrics_From_Traces.md) | TraceQL metrics, span metrics, PromQL, dashboards | 20 min |
| [5. Alerting](5_Alerting.md) | Contact points, alert rules, expression reference | 15 min |
| [6. Practical Reference Guide](6_Practical_Reference.md) | **Bookmark this** - debugging scenarios, quick reference | reference |

**Recommended path:** Complete parts 1-3 in order to learn the basics. Part 4-5 teach you how to track trends and set up alerts. Part 6 is a reference guide for everyday debugging - bookmark it.

## Quick Reference

After completing the tutorial, you'll use these most often:

**Find traces for an endpoint:**
```traceql
{span.http.route="/v2/projects/{projectId}"}
```

**Find slow requests:**
```traceql
{duration > 500ms}
```

**Find errors:**
```traceql
{status=error}
```

**Find database queries:**
```traceql
{span.db.system="postgresql"}
```

---

**Start:** [Part 1: Setting Up the Tracing Stack](1_Setting_Up_the_Tracing_Stack.md)
