# Import Performance Refactor

Reference for anyone working on `StoredDataImporter`, `ImportActivityRecorder`, or the import pipeline.

---

## Problem

Importing large numbers of keys (100k+) was extremely slow or failed entirely:

- **OOM at 1M keys**: The Hibernate activity interceptor accumulated `ActivityModifiedEntity` objects in `ActivityHolder.modifiedEntities` outside the Hibernate session. `flushAndClear()` did not clear them, so they piled up across the entire import and exhausted the heap.
- **Slow commit**: At transaction commit, `BeforeTransactionCompletionProcess` serialized and persisted all accumulated activity entities at once, taking 5+ minutes for 50k keys.
- **Memory pressure from import entities**: `ImportDataManager` loaded all `ImportKey` and `ImportTranslation` entities (3M+ at 1M keys) before the save loop, holding ~1.5 GB of heap plus ~2 GB of Hibernate persistence context overhead.

## Changes

### 1. Batched key/translation saving (`StoredDataImporter`)

Keys and translations are saved in batches of `FLUSH_BATCH_SIZE` (5000) with `flushAndClear()` after each batch. The `keysToSave` map is drained via its iterator as batches are processed, allowing GC to reclaim finished batches.

### 2. JDBC activity recording (`ImportActivityRecorder`)

Activity logging bypasses the Hibernate interceptor entirely. Each entity gets `disableActivityLogging = true` before persist, and activity rows are written via JDBC batch inserts after each `flushAndClear()`. This keeps the interceptor's state empty and persists activity incrementally.

The recorder writes `Key`, `KeyMeta`, and `Translation` entries to `activity_modified_entity`, plus describing entities to `activity_describing_entity`. Only newly created keys (tracked via an identity-based `newKeys` set) are recorded — pre-existing keys that receive updated translations are not.

The `ActivityRevision` is persisted via `entityManager.persist()` after the first batch's `flushAndClear()` to avoid `TransientObjectException` from unsaved Key entities.

### 3. Import data release (`releaseImportData`)

Before the batch loop, `ImportDataManager.storedKeys`, `storedTranslations`, and `translationsToUpdateDueToCollisions` are cleared. This releases ~6M `ImportKey`/`ImportTranslation` entities at 1M-key scale, freeing ~3.5 GB before the batch loop starts.

### 4. Auto-translation support

`enableAutoCompletion = false` prevents the interceptor's `BeforeTransactionCompletionProcess` from firing. Events are published manually:

- `OnProjectActivityEvent` is published via `publishOnProjectActivityEvent()` with a sentinel entry for `BranchRevisionListener`.
- `OnProjectActivityStoredEvent` carries lightweight `baseLanguageTranslations` records (collected during the batch loop) so `AutoTranslationEventHandler` can identify which keys need auto-translation and create a batch job.

### 5. Real-time progress reporting

The streaming import endpoint (`/apply-streaming`) reports batch progress via NDJSON. Each completed batch sends `importedKeys` and `totalKeys` counts. The frontend progress bar shows real progress instead of a fake CSS animation.

## Performance results

100k keys x 3 languages:

| Version | Step 2 (apply) |
|---------|---------------|
| Main branch (50k x 5, comparable scale) | 933s |
| Batches + JDBC activity recording | 428s |
| Batches + natural interceptor (no JDBC recorder) | 1077s |

1M keys x 3 languages: completes in ~47 min with peak memory ~7.5 GB (previously OOM'd with 8 GB heap).

## Key files

- `StoredDataImporter.kt` — batch loop, import data release, event publishing
- `ImportActivityRecorder.kt` — JDBC-based activity recording for Key, KeyMeta, Translation
- `ImportApplicationStatusItem.kt` — streaming progress data (totalKeys, importedKeys)
- `StreamingImportProgressUtil.kt` — NDJSON status streaming
