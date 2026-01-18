package io.tolgee.controllers.internal

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.NoOpRequest
import io.tolgee.hateoas.batch.BatchJobModel
import io.tolgee.hateoas.batch.BatchJobModelAssembler
import io.tolgee.model.batch.BatchJob
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@InternalController(["internal/batch-jobs"])
class InternalBatchJobController(
  private val batchJobService: BatchJobService,
  private val batchJobModelAssembler: BatchJobModelAssembler,
) {
  @PostMapping("/start-no-op")
  fun startNoOpJob(
    @Valid @RequestBody
    data: NoOpRequest,
  ): BatchJobModel {
    return batchJobService
      .startJob(
        data,
        project = null,
        author = null,
        type = BatchJobType.NO_OP,
      ).model
  }

  private val BatchJob.model
    get() = batchJobModelAssembler.toModel(batchJobService.getView(this))
}
