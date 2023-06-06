package io.tolgee.dtos

data class BatchJobChunkMessage(
  val batchJobId: Long,
  val chunkNumber: Int,
  var retries: Int = 0,
)
