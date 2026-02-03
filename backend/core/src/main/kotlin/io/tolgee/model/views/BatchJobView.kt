package io.tolgee.model.views

import io.tolgee.constants.Message
import io.tolgee.model.batch.BatchJob

class BatchJobView(
  val batchJob: BatchJob,
  val progress: Int,
  val errorMessage: Message?,
)
