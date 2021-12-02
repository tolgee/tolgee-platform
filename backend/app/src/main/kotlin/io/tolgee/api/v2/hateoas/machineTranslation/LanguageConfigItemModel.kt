package io.tolgee.api.v2.hateoas.machineTranslation

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "languageConfigs", itemRelation = "languageConfig")
class LanguageConfigItemModel(
  @Schema(description = "When null, its a default configuration applied to not configured languages")
  val targetLanguageId: Long?,

  @Schema(description = "When null, its a default configuration applied to not configured languages")
  val targetLanguageTag: String?,

  @Schema(description = "When null, its a default configuration applied to not configured languages")
  val targetLanguageName: String?,

  @Schema(description = "Service used for automated translating")
  val primaryService: MtServiceType?,

  @Schema(description = "Services to be used for suggesting")
  val enabledServices: Set<MtServiceType>
) : RepresentationModel<LanguageConfigItemModel>(), Serializable
