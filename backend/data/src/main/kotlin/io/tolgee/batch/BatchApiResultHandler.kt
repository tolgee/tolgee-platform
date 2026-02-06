package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto

/**
 * Interface for applying batch API results, implemented in the EE module.
 * Used by [processors.MachineTranslationChunkProcessor] to apply downloaded results
 * from the OpenAI Batch API without creating a compile-time dependency on EE code.
 */
interface BatchApiResultHandler {
  fun applyResults(
    job: BatchJobDto,
    chunkExecutionId: Long,
  )
}
