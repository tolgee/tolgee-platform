package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.dtos.WithRelatedKeysInOrder
import io.tolgee.model.enums.AssignableTranslationState
import io.tolgee.util.getSafeNamespace
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated

@Validated
data class ComplexEditKeyDto(
  @Schema(description = "Name of the key")
  @field:NotBlank
  @field:Length(max = 2000, min = 1)
  val name: String = "",
  @field:Length(max = 100)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  var namespace: String? = null,
  @Schema(description = "Translations to update")
  val translations: Map<String, String?>? = null,
  @Schema(description = "Translation states to update, if not provided states won't be modified")
  val states: Map<String, AssignableTranslationState>? = null,
  @Schema(description = "Tags of the key. If not provided tags won't be modified")
  val tags: List<String>? = null,
  @Schema(description = "IDs of screenshots to delete")
  val screenshotIdsToDelete: List<Long>? = null,
  @Schema(description = "Ids of screenshots uploaded with /v2/image-upload endpoint")
  @Deprecated("Use screenshotsToAdd instead")
  val screenshotUploadedImageIds: List<Long>? = null,
  val screenshotsToAdd: List<KeyScreenshotDto>? = null,
  override var relatedKeysInOrder: MutableList<RelatedKeyDto>? = null,
  @field:Size(max = 2000)
  @Schema(description = "Description of the key. It's also used as a context for Tolgee AI translator")
  val description: String? = null,
  @Schema(
    description =
      "If key is pluralized. If it will be reflected in the editor. " +
        "If null, value won't be modified.",
  )
  val isPlural: Boolean? = null,
  @Schema(
    description =
      "The argument name for the plural. " +
        "If null, value won't be modified. " +
        "If isPlural is false, this value will be ignored.",
  )
  val pluralArgName: String? = null,
  @Schema(
    description =
      "If true, it will fail with 400 (with code plural_forms_data_loss) " +
        "if plural is disabled and there are plural forms, " +
        "which would be lost by the action. You can get rid of this warning by setting this value to false.",
  )
  val warnOnDataLoss: Boolean? = false,
  @Schema(description = "Custom values of the key. If not provided, custom values won't be modified")
  val custom: Map<String, Any?>? = null,
) : WithRelatedKeysInOrder {
  @JsonSetter("namespace")
  fun setJsonNamespace(namespace: String?) {
    this.namespace = getSafeNamespace(namespace)
  }
}
