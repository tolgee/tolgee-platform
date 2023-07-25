package io.tolgee.batch

import io.tolgee.activity.data.ActivityType
import io.tolgee.batch.processors.AutoTranslationChunkProcessor
import io.tolgee.batch.processors.ClearTranslationsChunkProcessor
import io.tolgee.batch.processors.CopyTranslationsChunkProcessor
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.SetKeysNamespaceChunkProcessor
import io.tolgee.batch.processors.SetTranslationsStateChunkProcessor
import io.tolgee.batch.processors.TagKeysChunkProcessor
import io.tolgee.batch.processors.UntagKeysChunkProcessor
import kotlin.reflect.KClass

enum class BatchJobType(
  val activityType: ActivityType,
  /**
   * 0 means no chunking
   */
  val chunkSize: Int,
  val maxRetries: Int,
  val processor: KClass<out ChunkProcessor<*>>,
  val defaultRetryWaitTimeInMs: Int = 2000,
) {
  AUTO_TRANSLATION(
    activityType = ActivityType.BATCH_AUTO_TRANSLATE,
    chunkSize = 10,
    maxRetries = 3,
    processor = AutoTranslationChunkProcessor::class,
  ),
  DELETE_KEYS(
    activityType = ActivityType.KEY_DELETE,
    chunkSize = 0,
    maxRetries = 3,
    processor = DeleteKeysChunkProcessor::class,
  ),
  SET_TRANSLATIONS_STATE(
    activityType = ActivityType.BATCH_SET_TRANSLATION_STATE,
    chunkSize = 0,
    maxRetries = 3,
    processor = SetTranslationsStateChunkProcessor::class,
  ),
  CLEAR_TRANSLATIONS(
    activityType = ActivityType.BATCH_CLEAR_TRANSLATIONS,
    chunkSize = 0,
    maxRetries = 3,
    processor = ClearTranslationsChunkProcessor::class,
  ),
  COPY_TRANSLATIONS(
    activityType = ActivityType.BATCH_COPY_TRANSLATIONS,
    chunkSize = 0,
    maxRetries = 3,
    processor = CopyTranslationsChunkProcessor::class,
  ),
  TAG_KEYS(
    activityType = ActivityType.BATCH_TAG_KEYS,
    chunkSize = 0,
    maxRetries = 3,
    processor = TagKeysChunkProcessor::class,
  ),
  UNTAG_KEYS(
    activityType = ActivityType.BATCH_UNTAG_KEYS,
    chunkSize = 0,
    maxRetries = 3,
    processor = UntagKeysChunkProcessor::class,
  ),
  SET_KEYS_NAMESPACE(
    activityType = ActivityType.BATCH_SET_KEYS_NAMESPACE,
    chunkSize = 0,
    maxRetries = 3,
    processor = SetKeysNamespaceChunkProcessor::class,
  );
}
