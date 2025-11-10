package io.tolgee.hateoas.translations.comments

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import io.tolgee.model.enums.TranslationCommentState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Suppress("unused")
@Relation(collectionRelation = "translationComments", itemRelation = "translationComment")
open class TranslationCommentModel(
  @Schema(description = "Id of translation comment record")
  val id: Long,
  @Schema(description = "Text of comment")
  val text: String,
  @Schema(description = "State of translation")
  val state: TranslationCommentState,
  @Schema(description = "User who created the comment")
  val author: SimpleUserAccountModel,
  @Schema(description = "Date when it was created")
  val createdAt: Date,
  @Schema(description = "Date when it was updated")
  val updatedAt: Date,
) : RepresentationModel<TranslationCommentModel>()
