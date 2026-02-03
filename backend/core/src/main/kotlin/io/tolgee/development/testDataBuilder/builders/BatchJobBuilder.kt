package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution

class BatchJobBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<BatchJob, BatchJobBuilder>() {
  class DATA {
    val chunkExecutions = mutableListOf<BatchJobChunkExecutionBuilder>()
  }

  var data = DATA()

  override var self: BatchJob =
    BatchJob().apply {
      project = projectBuilder.self
    }

  var targetProvider: (() -> List<Any>)? = null

  fun addChunkExecution(ft: FT<BatchJobChunkExecution>) = addOperation(data.chunkExecutions, ft)
}
