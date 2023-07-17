package io.tolgee.batch

import io.tolgee.activity.data.ActivityType
import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.SetTranslationStateChunkProcessor
import io.tolgee.batch.processors.TranslationChunkProcessor
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
  TRANSLATION(
    activityType = ActivityType.BATCH_AUTO_TRANSLATE,
    chunkSize = 10,
    maxRetries = 3,
    processor = TranslationChunkProcessor::class,
  ),
  DELETE_KEYS(
    activityType = ActivityType.KEY_DELETE,
    chunkSize = 0,
    maxRetries = 3,
    processor = DeleteKeysChunkProcessor::class,
  ),
  SET_TRANSLATION_STATE(
    activityType = ActivityType.SET_TRANSLATION_STATE,
    chunkSize = 0,
    maxRetries = 3,
    processor = SetTranslationStateChunkProcessor::class,
  );
}
