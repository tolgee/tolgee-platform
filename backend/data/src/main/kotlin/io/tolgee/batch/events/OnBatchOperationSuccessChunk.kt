package io.tolgee.batch.events

import io.tolgee.model.batch.BatchJob

data class OnBatchOperationSuccessChunk(
  val job: BatchJob,
  val chunkNum: Int
)
