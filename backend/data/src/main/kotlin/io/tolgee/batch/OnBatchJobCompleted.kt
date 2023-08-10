package io.tolgee.batch

import io.tolgee.batch.data.BatchJobDto

interface OnBatchJobCompleted {
  val job: BatchJobDto
}
