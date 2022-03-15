package io.tolgee.api.v2.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.user_account.SimpleUserAccountModel
import io.tolgee.constants.MtServiceType
import io.tolgee.model.enums.TranslationState
import org.hibernate.envers.RevisionType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "revisions", itemRelation = "revision")
open class TranslationHistoryModel(
  @Schema(description = "Translation text")
  val text: String?,

  @Schema(description = "State of translation")
  val state: TranslationState?,

  @Schema(description = "Was translated using Translation Memory or Machine translation service?")
  val auto: Boolean?,

  @Schema(description = "Which machine translation service was used to auto translate this")
  val mtProvider: MtServiceType?,

  @Schema(description = "Unix timestamp of the revision")
  val timestamp: Long,

  @Schema(description = "Author of the change")
  val author: SimpleUserAccountModel?,

  val revisionType: RevisionType
) : RepresentationModel<TranslationHistoryModel>()
