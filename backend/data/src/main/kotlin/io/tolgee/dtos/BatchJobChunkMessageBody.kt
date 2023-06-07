package io.tolgee.dtos

data class BatchJobChunkMessageBody(
  val batchJobId: Long,
  val chunkNumber: Int,
  var retries: Int = -1,
  var waitUntil: Long? = null,
)
