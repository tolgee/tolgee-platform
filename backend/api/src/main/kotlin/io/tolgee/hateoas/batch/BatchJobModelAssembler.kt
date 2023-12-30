package io.tolgee.hateoas.batch

import io.tolgee.api.v2.controllers.batch.BatchJobManagementController
import io.tolgee.hateoas.userAccount.SimpleUserAccountModelAssembler
import io.tolgee.model.views.BatchJobView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class BatchJobModelAssembler(
  val simpleUserAccountModelAssembler: SimpleUserAccountModelAssembler,
) : RepresentationModelAssemblerSupport<BatchJobView, BatchJobModel>(
    BatchJobManagementController::class.java,
    BatchJobModel::class.java,
  ) {
  override fun toModel(view: BatchJobView): BatchJobModel {
    return BatchJobModel(
      id = view.batchJob.id,
      type = view.batchJob.type,
      status = view.batchJob.status,
      progress = view.progress,
      totalItems = view.batchJob.totalItems,
      author = view.batchJob.author?.let { simpleUserAccountModelAssembler.toModel(it) },
      createdAt = view.batchJob.createdAt?.time ?: 0,
      updatedAt = view.batchJob.updatedAt?.time ?: 0,
      activityRevisionId = view.batchJob.activityRevision?.id,
      errorMessage = view.errorMessage?.code,
    )
  }
}
