package io.tolgee.batch

import kotlin.reflect.KClass

enum class BatchJobType(
  val chunkSize: Int,
  val maxRetries: Int,
  val processor: KClass<out ChunkProcessor<*>>,
  val defaultRetryWaitTimeInMs: Int = 2000,
) {
  TRANSLATION(
    chunkSize = 10,
    maxRetries = 3,
    processor = TranslationChunkProcessor::class,
  );
}
