package io.tolgee.batch

import io.tolgee.model.batch.BatchJob

data class OnBatchOperationSuccessChunk(
  val job: BatchJob,
  val chunkNum: Int
)
