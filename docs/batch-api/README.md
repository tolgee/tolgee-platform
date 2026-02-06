# OpenAI Batch API Subsystem

## What We Need

Three capabilities for cost-optimized bulk translations:

1. **Asynchronous bulk translation** - Submit large translation jobs to OpenAI's Batch API at 50% reduced cost
2. **Automatic polling and result retrieval** - Poll for completion and apply results without blocking user sessions or worker coroutines
3. **Graceful degradation** - Fall back to synchronous translation when the batch API is unavailable or fails

## How We Achieve It

| Capability | Solution |
|---|---|
| Batch submission | `BatchApiSubmissionService` builds JSONL, uploads via Files API, creates batch via Batches API |
| Two-phase execution | `WaitingForExternalException` signals the framework to set `WAITING_FOR_EXTERNAL` status, freeing the worker coroutine |
| Polling | `OpenAiBatchPoller` uses `SchedulingManager` + `LockingProvider` for testable, multi-instance-safe periodic polling |
| Result retrieval | Poller downloads JSONL output, parses results, stores them on the tracker entity, re-queues the chunk for Phase 2 |
| Result application | `BatchApiResultApplier` reads parsed results from the tracker and calls `TranslationService.setTranslationText()` |
| State tracking | `OpenAiBatchJobTracker` entity tracks the external batch lifecycle with optimistic locking (`@Version`) |
| Integration | `MachineTranslationChunkProcessor` routes to batch or sync based on `useBatchApi` job param |
| EE/CE separation | `BatchApiSubmitter` and `BatchApiResultHandler` interfaces in CE module; implementations in EE module |

## High-Level Architecture

```
User triggers batch machine translate (useBatchApi=true)
    |
    v
MachineTranslationChunkProcessor.processBatchApi()
    |
    |-- Phase 1 (no tracker exists for this chunk):
    |       |
    |       v
    |   BatchApiSubmissionService.submitBatch()
    |       |-- builds JSONL (custom_id = "{jobId}:{keyId}:{languageId}")
    |       |-- OpenAiBatchApiService.submitBatch()
    |       |       |-- POST /v1/files (upload JSONL)
    |       |       |-- POST /v1/batches (create batch)
    |       |-- saves OpenAiBatchJobTracker (status: SUBMITTED)
    |       |-- throws WaitingForExternalException
    |               |
    |               v
    |           ChunkProcessingUtil catches it
    |           Sets execution status to WAITING_FOR_EXTERNAL
    |           Worker coroutine is freed
    |
    |-- Phase 2 (tracker exists with status RESULTS_READY):
    |       |
    |       v
    |   BatchApiResultApplier.applyResults()
    |       |-- reads parsed results from tracker.results (JSONB)
    |       |-- for each result: TranslationService.setTranslationText()
    |       |-- reports progress via ProgressManager
    |       |-- sets tracker status to COMPLETED
    |
    v
OpenAiBatchPoller (runs on SchedulingManager)
    |-- acquires distributed lock (LockingProvider)
    |-- queries trackers with status SUBMITTED or IN_PROGRESS
    |-- for each tracker:
    |       |-- OpenAiBatchApiService.pollBatchStatus()
    |       |-- on "completed": download results, parse, store on tracker,
    |       |                    set tracker to RESULTS_READY,
    |       |                    re-queue chunk execution as PENDING
    |       |-- on "failed"/"expired": mark tracker FAILED, mark chunk FAILED
    |       |-- on "cancelled": mark tracker CANCELLED, mark chunk CANCELLED
    |       |-- on "in_progress"/"finalizing": update tracker status, continue
```

## Component Inventory

### Data Layer (`backend/data`)

| Class | Package | Purpose |
|---|---|---|
| `OpenAiBatchJobTracker` | `io.tolgee.model.batch` | JPA entity tracking external batch job state, results, and progress counters |
| `OpenAiBatchTrackerStatus` | `io.tolgee.model.batch` | Enum: SUBMITTED, IN_PROGRESS, RESULTS_READY, APPLYING, COMPLETED, FAILED, CANCELLED |
| `OpenAiBatchResult` | `io.tolgee.model.batch` | Data class for a single parsed result; stored as JSONB on the tracker |
| `OpenAiBatchJobTrackerRepository` | `io.tolgee.repository.batch` | JPA repository with `PESSIMISTIC_WRITE` + `SKIP LOCKED` query for polling |
| `BatchJobChunkExecutionStatus` | `io.tolgee.model.batch` | Extended with `WAITING_FOR_EXTERNAL(false)` non-terminal status |
| `WaitingForExternalException` | `io.tolgee.batch` | Thrown to signal chunk is waiting for external results |
| `BatchApiSubmitter` | `io.tolgee.batch` | Interface for batch submission (CE module boundary) |
| `BatchApiResultHandler` | `io.tolgee.batch` | Interface for result application (CE module boundary) |
| `MachineTranslationJobParams` | `io.tolgee.model.batch.params` | Extended with `useBatchApi: Boolean = false` |
| `BatchProperties` | `io.tolgee.configuration.tolgee` | Extended with `batchApiPollIntervalSeconds`, `batchApiMaxWaitHours`, `batchApiChunkSize`, etc. |
| `LlmProviderInterface` | `io.tolgee.configuration.tolgee.machineTranslation` | Extended with `batchApiEnabled`, `batchTokenPriceInCreditsInput/Output` |

### EE Service Layer (`ee/backend/app`)

| Class | Package | Purpose |
|---|---|---|
| `OpenAiBatchApiService` | `io.tolgee.ee.service` | Interface abstracting the OpenAI Batch API lifecycle |
| `OpenAiBatchApiServiceImpl` | `io.tolgee.ee.service` | Production implementation using `RestTemplate`; delegates to `FakeBatchApiDelegate` when `fakeBatchApi=true` |
| `BatchApiSubmissionService` | `io.tolgee.ee.service.batch` | Implements `BatchApiSubmitter`; builds JSONL, submits batch, creates tracker, throws `WaitingForExternalException` |
| `OpenAiBatchPoller` | `io.tolgee.ee.service.batch` | `SchedulingManager`-based poller; polls OpenAI, downloads results, re-queues chunks |
| `BatchApiResultApplier` | `io.tolgee.ee.service.batch` | Implements `BatchApiResultHandler`; applies parsed results to translations |
| `FakeBatchApiDelegate` | `io.tolgee.ee.component` | Interface for in-process fake API (E2E/dev mode) |

### Modified Framework Classes

| Class | Change |
|---|---|
| `ChunkProcessingUtil` | Catches `WaitingForExternalException` and sets `WAITING_FOR_EXTERNAL` status |
| `ProgressManager.handleChunkCompletedCommitted()` | Guards against non-terminal statuses; skips committed count for `WAITING_FOR_EXTERNAL` |
| `MachineTranslationChunkProcessor` | Routes to `BatchApiSubmitter` (Phase 1) or `BatchApiResultHandler` (Phase 2) when `useBatchApi=true`; overrides `getChunkSize()` for batch mode |

### Test Infrastructure

| Class | Package | Purpose |
|---|---|---|
| `FakeOpenAiBatchApiService` | `io.tolgee.ee.unit.batch` | `@Primary @Profile("test")` replacement with configurable state machine, error injection, and assertion tracking |

---

## State Machine Reference

### OpenAiBatchTrackerStatus Transitions

```
SUBMITTED
  |-- poller sees "in_progress"/"finalizing" --> IN_PROGRESS
  |-- poller sees "completed"                --> RESULTS_READY
  |-- poller sees "failed"                   --> FAILED
  |-- poller sees "expired"                  --> FAILED (with error message)
  |-- poller sees "cancelled"/"cancelling"   --> CANCELLED

IN_PROGRESS
  |-- poller sees "completed"                --> RESULTS_READY
  |-- poller sees "failed"                   --> FAILED
  |-- poller sees "expired"                  --> FAILED
  |-- poller sees "cancelled"/"cancelling"   --> CANCELLED

RESULTS_READY
  |-- chunk re-queued, Phase 2 starts        --> APPLYING

APPLYING
  |-- all results applied                    --> COMPLETED

COMPLETED  (terminal)
FAILED     (terminal)
CANCELLED  (terminal)
```

### OpenAI Status to Internal Status Mapping

| OpenAI `status` field | Internal `OpenAiBatchTrackerStatus` | Side effects |
|---|---|---|
| `validating` | (no change, stays SUBMITTED) | Logged at debug level |
| `in_progress` | `IN_PROGRESS` | Progress counters updated |
| `finalizing` | `IN_PROGRESS` | Treated same as in_progress |
| `completed` | `RESULTS_READY` | Results downloaded, parsed, stored; chunk re-queued as PENDING |
| `failed` | `FAILED` | Error message recorded; chunk execution set to FAILED |
| `expired` | `FAILED` | Error "Batch expired at OpenAI"; chunk execution set to FAILED |
| `cancelling` | `CANCELLED` | Chunk execution set to CANCELLED |
| `cancelled` | `CANCELLED` | Chunk execution set to CANCELLED |

### Chunk Execution Status Flow (Batch Mode)

```
PENDING  -->  RUNNING  -->  WAITING_FOR_EXTERNAL  (Phase 1: submitted to OpenAI)
                                   |
                          [poller downloads results, re-queues]
                                   |
                                   v
                               PENDING  -->  RUNNING  -->  SUCCESS  (Phase 2: results applied)
                                                       |
                                                       v
                                                     FAILED  (if application fails)
```

---

## Configuration Model

### Two-Tier Configuration (YAML + DB)

The LLM provider system uses a two-source configuration model. Batch API fields follow the same pattern:

```
YAML config (tolgee.llm.providers[])
    |
    v
LlmProperties.LlmProvider (@ConfigurationProperties)
    |-- toDto(id) --> LlmProviderDto (negative ID: -1, -2, ...)
    |
    v
                   LlmProviderDto (implements LlmProviderInterface)
                   ^
DB entity (llm_provider table)
    |
    |-- toDto() --> LlmProviderDto (positive ID from DB)
```

**Resolution rules** (in `LlmProviderService.getProviderByName()`):
1. If a DB-configured provider exists with the requested name, use DB providers only
2. Otherwise fall back to YAML-configured providers
3. Filter by priority if a matching priority exists
4. Round-robin across remaining providers
5. Skip providers that are currently rate-limited

### Batch API Fields on LlmProviderInterface

| Field | Type | Description |
|---|---|---|
| `batchApiEnabled` | `Boolean?` | Whether this provider supports batch API submission |
| `batchTokenPriceInCreditsInput` | `Double?` | Input token price for batch (lower than sync price for discount) |
| `batchTokenPriceInCreditsOutput` | `Double?` | Output token price for batch |

### Batch Properties (tolgee.batch.*)

| Property | Type | Default | Description |
|---|---|---|---|
| `batchApiPollIntervalSeconds` | `Int` | `60` | Seconds between poll cycles |
| `batchApiMaxWaitHours` | `Int` | `24` | Hours before timeout fallback |
| `batchApiChunkSize` | `Int` | `5000` | Translation items per batch file chunk |
| `batchApiMaxItemsPerJob` | `Int` | `50000` | Max items in a single batch API job |
| `batchApiMaxConcurrentPerOrg` | `Int` | `5` | Max concurrent batches per organization |
| `batchApiMaxConcurrentGlobal` | `Int` | `100` | Max concurrent batches globally |
| `batchApiMaxPollBatchSize` | `Int` | `50` | Max trackers polled per cycle |

---

## Polling Mechanism

### Why SchedulingManager (not @Scheduled)

Spring context caching in tests causes `@Scheduled` methods to fire across test executions. `SchedulingManager` allows:
- Per-test control (manual cancellation)
- Configurable poll intervals
- Clean shutdown via `@PreDestroy`

This follows the pattern established by `OldBatchJobCleaner` and `ScheduledReportingManager`.

### Multi-Instance Safety

| Aspect | Detail |
|---|---|
| Lock name | `openai_batch_poller_lock` |
| Lock duration | 5 minutes |
| Lock provider | `LockingProvider` (Redisson in prod, JVM ReentrantLock in dev) |
| Behavior | `withLockingIfFree()` -- non-blocking; if lock is held, this instance's poll cycle is a no-op |
| DB-level safety | `findAndLockByStatusIn()` uses `PESSIMISTIC_WRITE` + `SKIP LOCKED` (lock timeout = -2 means skip locked rows) |

### Poll Cycle Flow

1. `SchedulingManager` fires `pollAllPending()` at the configured interval
2. `LockingProvider.withLockingIfFree()` acquires the distributed lock (or skips)
3. New transaction: query all trackers with status SUBMITTED or IN_PROGRESS
4. For each tracker (each in its own transaction):
   - Fetch provider config (API key, URL)
   - Call `OpenAiBatchApiService.pollBatchStatus()`
   - Update tracker based on response status
   - If completed: download results, parse, store on tracker, re-queue chunk
   - If failed/expired: mark tracker and chunk as failed
   - If cancelled: mark tracker and chunk as cancelled

---

## JSONL Format

### Request (uploaded to OpenAI)

Each line is a JSON object with the OpenAI Batch API request format:

```json
{
  "custom_id": "{jobId}:{keyId}:{languageId}",
  "method": "POST",
  "url": "/v1/chat/completions",
  "body": {
    "model": "gpt-4o-mini",
    "messages": [
      {"role": "system", "content": "You are a professional translator..."},
      {"role": "user", "content": "Translate the key 'greeting' to cs."}
    ]
  }
}
```

The `custom_id` encodes `{jobId}:{keyId}:{languageId}` so results can be mapped back to the correct translation key and language.

### Response (downloaded from OpenAI)

Each line is a JSON object with the OpenAI Batch API response format:

```json
{
  "id": "batch_req_1",
  "custom_id": "42:100:5",
  "response": {
    "status_code": 200,
    "request_id": "req_abc",
    "body": {
      "choices": [{"message": {"content": "translated text"}}],
      "usage": {"prompt_tokens": 50, "completion_tokens": 15}
    }
  },
  "error": null
}
```

The poller parses `custom_id` to extract `keyId` and `languageId`, and reads `response.body.choices[0].message.content` for the translated text. Token usage from `response.body.usage` is stored for billing.

---

## Developer Extension Guide: Adding Batch API Support for a New Provider

### Prerequisites

- The LLM provider must offer a batch/async API
- The provider type must exist in `LlmProviderType` enum

### Steps

1. **Create a new `OpenAiBatchApiService`-like interface** (or reuse it if the API shape is compatible). The key operations are: submit batch, poll status, download results, cancel, and delete files.

2. **Implement the service** for the new provider (equivalent of `OpenAiBatchApiServiceImpl`). Handle authentication, JSONL format differences, and error mapping.

3. **Create a fake implementation** for tests (equivalent of `FakeOpenAiBatchApiService`). Provide:
   - In-memory file and batch stores
   - Configurable completion modes (instant, delayed, failed)
   - Assertion tracking (submissions, poll calls, cancellations)
   - `reset()` method for test isolation

4. **Update `BatchApiSubmissionService`** to detect the provider type and delegate to the appropriate batch API service. Currently it hardcodes `"openai"` for provider lookup.

5. **Update `OpenAiBatchPoller`** to handle the new provider's status values in the `when (statusResult.status)` block. Different providers may use different status strings.

6. **Add configuration fields** if the provider requires provider-specific batch properties.

7. **Write tests** using the fake service:
   - Unit tests for JSONL generation and result parsing
   - Integration tests for the full lifecycle (submit, poll, apply)
   - Error handling tests (submission failure, poll failure, partial results)

---

## Testing Guide

### FakeOpenAiBatchApiService

The test fake replaces `OpenAiBatchApiService` via `@Primary @Profile("test")`. It provides a configurable state machine:

```
validating -> in_progress -> finalizing -> completed
```

**Configuration knobs:**

| Property | Effect |
|---|---|
| `instantCompletion = true` | Batches complete immediately on creation (skip intermediate states) |
| `completionDelayMs = N` | Batches complete after N ms in `in_progress` state |
| `nextCreateError = Exception(...)` | Next `submitBatch()` call throws the configured exception |
| `nextPollError = Exception(...)` | Next `pollBatchStatus()` call throws the configured exception |
| `failedItemCount = N` | N items reported as failed in `RequestCounts` |
| `stuckInValidating = true` | Batches stay in `validating` and never advance |

**Assertion tracking:**

| Field | What it captures |
|---|---|
| `submissions` | All `submitBatch()` calls with full arguments |
| `pollCalls` | All `pollBatchStatus()` calls |
| `cancelCalls` | All `cancelBatch()` batch IDs |
| `deletedFiles` | All `deleteFile()` file IDs |

**Manual helpers:**

| Method | Effect |
|---|---|
| `completeBatch(batchId)` | Force-completes a batch (generates output, sets terminal state) |
| `failBatch(batchId, msg)` | Force-fails a batch with the given error message |
| `expireBatch(batchId)` | Force-expires a batch |
| `reset()` | Clears all state and configuration (call in `@BeforeEach`) |

### Test Pattern

```kotlin
// 1. Set up test data
val testData = BatchJobsTestData(...)
testDataService.saveTestData(testData.root)

// 2. Configure the fake
fakeOpenAiBatchApiService.instantCompletion = true

// 3. Trigger batch translation
performProjectAuthPost(
  "start-batch-job/machine-translate",
  MachineTranslationRequest(
    keyIds = listOf(key1.id, key2.id),
    targetLanguageIds = listOf(czech.id),
    useBatchApi = true,
  ),
)

// 4. Wait for job to complete
batchJobTestUtil.waitForCompleted(job)

// 5. Assert
assertThat(fakeOpenAiBatchApiService.submissions).hasSize(1)
assertThat(translationService.find(key1, czech)?.text).isNotNull()
```

### Test Categories

1. **Happy path**: Submit, poll advances through states, results applied
2. **Submission failure**: `nextCreateError` set, verify chunk falls back or fails
3. **Processing failure**: `failBatch()` called, verify error propagation
4. **Expiry**: `expireBatch()` called, verify timeout handling
5. **Cancellation**: Cancel job while batch is in progress, verify `cancelCalls`
6. **Partial failure**: `failedItemCount > 0`, verify successful items still applied
7. **Configuration merge**: Verify YAML/DB merge produces correct `batchApiEnabled` and pricing fields

### Time Control

Use `CurrentDateProvider.forcedDate` to advance time (existing pattern in `BatchJobTestUtil`). The fake uses `currentDateProvider.date.time` for all timestamps, so advancing the date affects completion delay calculations.

---

## Database Schema

### openai_batch_job_tracker

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `bigint` | PK, auto-generated | |
| `created_at` | `timestamp` | not null | Audit: creation time |
| `updated_at` | `timestamp` | not null | Audit: last update time |
| `batch_job_id` | `bigint` | FK -> batch_job, not null | Parent batch job |
| `chunk_execution_id` | `bigint` | FK -> batch_job_chunk_execution, not null | Associated chunk execution |
| `openai_batch_id` | `varchar` | unique, not null | OpenAI's batch ID |
| `openai_input_file_id` | `varchar` | not null | OpenAI's input file ID |
| `openai_output_file_id` | `varchar` | nullable | OpenAI's output file ID (set on completion) |
| `openai_error_file_id` | `varchar` | nullable | OpenAI's error file ID |
| `status` | `varchar` | not null | `OpenAiBatchTrackerStatus` enum value |
| `openai_status` | `varchar` | nullable | Raw status string from OpenAI |
| `total_requests` | `int` | default 0 | Total requests in the batch |
| `completed_requests` | `int` | default 0 | Successfully completed requests |
| `failed_requests` | `int` | default 0 | Failed requests |
| `results` | `jsonb` | nullable | Parsed `List<OpenAiBatchResult>` |
| `error_message` | `text` | nullable | Error details for failed batches |
| `provider_id` | `bigint` | nullable | LLM provider ID used for this batch |
| `version` | `bigint` | not null, default 0 | Optimistic locking version |

**Indexes:**
- `batch_job_id`
- `status`
- `openai_batch_id`
