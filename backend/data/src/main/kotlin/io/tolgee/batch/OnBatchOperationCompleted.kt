package io.tolgee.batch

import io.tolgee.model.batch.BatchJob

interface OnBatchOperationCompleted {
  val job: BatchJob
}
