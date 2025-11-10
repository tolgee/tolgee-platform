package io.tolgee.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType
import io.tolgee.hateoas.label.LabelModel
import io.tolgee.hateoas.translations.suggestions.TranslationSuggestionSimpleModel
import io.tolgee.model.enums.TranslationState
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "translations", itemRelation = "translation")
open class TranslationViewModel(
  @Schema(description = "Id of translation record")
  val id: Long?,
  @Schema(description = "Translation text")
  val text: String?,
  @Schema(description = "State of translation")
  val state: TranslationState,
  @Schema(description = "Whether base language translation was changed after this translation was updated")
  val outdated: Boolean,
  @Schema(description = "Was translated using Translation Memory or Machine translation service?")
  val auto: Boolean,
  @Schema(description = "Which machine translation service was used to auto translate this")
  val mtProvider: MtServiceType?,
  @Schema(description = "Count of translation comments")
  val commentCount: Long,
  @Schema(description = "Count of unresolved translation comments")
  val unresolvedCommentCount: Long,
  @Schema(description = "Labels assigned to this translation")
  val labels: List<LabelModel>?,
  @Schema(description = "Number of active suggestions")
  val activeSuggestionCount: Long,
  @Schema(description = "Number of all suggestions")
  val totalSuggestionCount: Long,
  @get:Schema(description = "First suggestion")
  val suggestions: List<TranslationSuggestionSimpleModel>? = null,
) : RepresentationModel<TranslationViewModel>() {
  @get:Schema(description = "Was translation memory used to translate this?")
  val fromTranslationMemory: Boolean
    get() = auto && mtProvider == null
}
