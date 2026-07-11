package io.tolgee.hateoas.machineTranslation

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType
import io.tolgee.service.machineTranslation.MtServiceInfo
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
  @Schema(
    description = "Service used for automated translating (deprecated: use primaryServiceInfo)",
    deprecated = true,
  )
  val primaryService: MtServiceType?,
  @Schema(description = "Service info used for automated translating")
  val primaryServiceInfo: MtServiceInfo?,
  @Schema(
    description = "Services to be used for suggesting (deprecated: use enabledServicesInfo)",
    deprecated = true,
  )
  val enabledServices: Set<MtServiceType>,
  @Schema(description = "Info about enabled services")
  var enabledServicesInfo: Set<MtServiceInfo>,
) : RepresentationModel<LanguageConfigItemModel>(),
  Serializable
