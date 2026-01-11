package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.MtServiceType
import io.tolgee.service.machineTranslation.MtServiceInfo

data class MachineTranslationLanguagePropsDto(
  @Schema(description = "The language to apply those rules. If null, then this settings are default.")
  var targetLanguageId: Long? = null,
  @Schema(description = "This service will be used for automated translation", deprecated = true)
  var primaryService: MtServiceType? = null,
  @Schema(description = "This service info will be used for automated translation")
  var primaryServiceInfo: MtServiceInfo? = null,
  @Schema(description = "List of enabled services (deprecated: use enabledServicesInfo)", deprecated = true)
  var enabledServices: Set<MtServiceType>? = setOf(),
  @Schema(description = "Info about enabled services")
  var enabledServicesInfo: Set<MtServiceInfo>? = setOf(),
) {
  /*
   * The OpenApi generator doesn't support default values and marks property as required when non nullable type is used,
   * so we need nullable types and add these custom setters
   */
  @get:JsonIgnore
  val enabledServicesInfoNotNull: Set<MtServiceInfo>
    get() = enabledServicesInfo ?: setOf()

  @get:JsonIgnore
  val enabledServicesNotNull: Set<MtServiceType>
    get() = enabledServices ?: setOf()
}
