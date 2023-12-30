package io.tolgee.batch.data

data class JobUnlockedChunk(
  val batchJobId: Long,
  val batchJobChunkExecutionId: Long,
)
