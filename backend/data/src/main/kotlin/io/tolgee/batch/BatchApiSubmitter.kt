package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.model.batch.BatchJobChunkExecution

/**
 * Interface for batch API submission, implemented in the EE module.
 * Used by [processors.MachineTranslationChunkProcessor] to submit chunks
 * to the OpenAI Batch API without creating a compile-time dependency on EE code.
 */
interface BatchApiSubmitter {
  fun submitBatch(
    job: BatchJobDto,
    chunk: List<BatchTranslationTargetItem>,
    chunkExecution: BatchJobChunkExecution,
  )
}
