package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.batch.BatchJobChunkExecution

class BatchJobChunkExecutionBuilder(
  val batchJobBuilder: BatchJobBuilder,
) : EntityDataBuilder<BatchJobChunkExecution, BatchJobChunkExecutionBuilder> {
  override var self: BatchJobChunkExecution =
    BatchJobChunkExecution().apply {
      this@apply.batchJob = this@BatchJobChunkExecutionBuilder.batchJobBuilder.self
    }

  var successfulTargetsProvider: (() -> List<Any>)? = null
}
