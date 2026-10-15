package io.tolgee.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.data.RevisionType
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "revisions", itemRelation = "revision")
open class TranslationHistoryModel(
  @Schema(description = "Modified fields")
  val modifications: Map<String, PropertyModification>? = null,
  @Schema(description = "Unix timestamp of the revision")
  val timestamp: Long,
  @Schema(description = "Author of the change")
  val author: SimpleUserAccountModel?,
  val revisionType: RevisionType,
  @Schema(description = "Type of the activity that made the change. Changes made by AI review have no author.")
  val activityType: ActivityType? = null,
) : RepresentationModel<TranslationHistoryModel>()
