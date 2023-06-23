package io.tolgee.batch

import io.tolgee.batch.processors.DeleteKeysChunkProcessor
import io.tolgee.batch.processors.TranslationChunkProcessor
import kotlin.reflect.KClass

enum class BatchJobType(
  /**
   * 0 means no chunking
   */
  val chunkSize: Int,
  val maxRetries: Int,
  val processor: KClass<out ChunkProcessor<*>>,
  val defaultRetryWaitTimeInMs: Int = 2000,
) {
  TRANSLATION(
    chunkSize = 10,
    maxRetries = 3,
    processor = TranslationChunkProcessor::class,
  ),
  DELETE_KEYS(
    chunkSize = 0,
    maxRetries = 3,
    processor = DeleteKeysChunkProcessor::class,
  );
}
