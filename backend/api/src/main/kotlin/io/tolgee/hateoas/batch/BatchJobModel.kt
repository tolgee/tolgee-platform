package io.tolgee.hateoas.batch

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.batch.data.BatchJobType
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.batch.BatchJobStatus
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "batchJobs", itemRelation = "batchJob")
open class BatchJobModel(
  @Schema(description = "Batch job id")
  val id: Long,
  @Schema(description = "Status of the batch job")
  val status: BatchJobStatus,
  @Schema(description = "Type of the batch job")
  val type: BatchJobType,
  @Schema(description = "Total items, that have been processed so far")
  val progress: Int,
  @Schema(description = "Total items")
  val totalItems: Int,
  @Schema(description = "The user who started the job")
  val author: SimpleUserAccountModel?,
  @Schema(description = "The time when the job created")
  val createdAt: Long,
  @Schema(description = "The time when the job was last updated (status change)")
  val updatedAt: Long,
  @Schema(description = "The activity revision id, that stores the activity details of the job")
  val activityRevisionId: Long?,
  @Schema(description = "If the job failed, this is the error message")
  val errorMessage: String?,
) : RepresentationModel<BatchJobModel>(),
  Serializable
