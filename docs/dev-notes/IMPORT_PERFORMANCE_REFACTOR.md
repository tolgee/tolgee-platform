# Import Performance Refactor

Reference for anyone working on `StoredDataImporter`, activity recording, or the import pipeline.

---

## Problem

Importing large numbers of keys (100k+) was extremely slow or failed entirely:

- **OOM at 1M keys**: The Hibernate activity interceptor accumulated `ActivityModifiedEntity` objects in `ActivityHolder.modifiedEntities` outside the Hibernate session. `flushAndClear()` did not clear them, so they piled up across the entire import and exhausted the heap.
- **Slow commit**: At transaction commit, `BeforeTransactionCompletionProcess` serialized and persisted all accumulated activity entities at once, taking 5+ minutes for 50k keys.
- **Memory pressure from import entities**: `ImportDataManager` loaded all `ImportKey` and `ImportTranslation` entities (3M+ at 1M keys) before the save loop, holding ~1.5 GB of heap plus ~2 GB of Hibernate persistence context overhead.

## Changes

### 1. Batched key/translation saving (`StoredDataImporter`)

Keys and translations are saved in configurable batches (`ImportProperties.flushBatchSize`, default 10,000) with `flushAndClear()` after each batch. Per-language import data is released as soon as it's consumed, and the full parsed-file graph is released before the persist phase.

### 2. Compact activity-recording memory footprint

Instead of bypassing the Hibernate interceptor, the activity recording structures were made lightweight:

- **`PropertyModifications`** — replaces `Map<String, PropertyModification>` with a compact layout that shares field-name strings via a per-entity-class `NameSchema` registry. Same JSONB shape on disk.
- **`CompactSharedMap`** / **`DescribingDataMap`** / **`DescribingRelationsMap`** — compact map implementations backing `ActivityModifiedEntity` JSONB columns without per-row map overhead.
- **`ModifiedCollectionKey`** — stores `(entityClass, entityId, fieldName)` instead of `(entityInstance, fieldName)` so `flushAndClear()` can actually free entity objects.
- **`@Immutable` on `ActivityModifiedEntity`** — skips dirty-checking since the entity is insert-only.

This lets the natural Hibernate interceptor work at scale without OOM.

### 3. Import data release

- **Per-language release**: `ImportDataManager.releaseLanguageData()` drops one language's `ImportTranslation` graph as soon as the importer has consumed it.
- **Full release**: `ImportDataManager.releaseData()` clears all parsed-file data (`storedKeys`, `storedTranslations`, `storedTranslationsByKeyName`) once keys and translations have been extracted into the importer's working structures.

### 4. Hotspot fixes

- **`TranslationService.setQaChecksStale`**: uses `ANY(:ids)` with a single `bigint[]` parameter instead of expanding to thousands of `?` placeholders. Added `AND qa_checks_stale = false` to skip no-op writes.
- **`WordCounter`**: samples a bounded prefix for word counting.
- **`FormatDetectionUtil`**: caps detection sampling.
- **`QaActivityListener`**: pre-filters to base-language translations in Kotlin before issuing SQL.

### 5. Real-time progress reporting

The streaming import endpoint (`/apply-streaming`) reports batch progress via NDJSON. Each completed batch sends `importedKeys` and `totalKeys` counts. The frontend progress bar shows real progress instead of a fake CSS animation.

## Performance results

100k keys x 3 languages:

| Version | Step 2 (apply) |
|---------|---------------|
| Main branch (50k x 5, comparable scale) | 933s |
| Batched persist + compact activity maps | 380-390s |

1M keys x 3 languages: completes in ~121 min with peak memory ~538 MB (previously OOM'd with 8 GB heap).

## Key files

- `StoredDataImporter.kt` — batch loop, import data release, progress reporting
- `ImportDataManager.kt` — parsed-file data management, per-language release
- `ActivityModifiedEntity.kt` — `@Immutable`, compact map fields
- `CompactSharedMap.kt` / `NameSchema.kt` / `PropertyModifications.kt` — compact activity data structures
- `ImportApplicationStatusItem.kt` — streaming progress data (totalKeys, importedKeys)
- `StreamingImportProgressUtil.kt` — NDJSON status streaming
