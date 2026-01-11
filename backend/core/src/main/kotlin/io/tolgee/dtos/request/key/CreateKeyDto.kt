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
class CreateKeyDto(
  /**
   * Key full path is stored as name in entity
   */
  @Schema(description = "Name of the key")
  @field:NotBlank
  @field:Length(max = 2000, min = 1)
  val name: String = "",
  @field:Length(max = 100)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  var namespace: String? = null,
  val translations: Map<String, String?>? = null,
  @Schema(description = "Translation states to update, if not provided states won't be modified")
  val states: Map<String, AssignableTranslationState>? = null,
  val tags: List<String>? = null,
  @Schema(description = "Ids of screenshots uploaded with /v2/image-upload endpoint")
  @Deprecated("Use screenshots instead")
  val screenshotUploadedImageIds: List<Long>? = null,
  val screenshots: List<KeyScreenshotDto>? = null,
  override var relatedKeysInOrder: MutableList<RelatedKeyDto>? = null,
  @field:Size(max = 2000)
  @Schema(
    description = "Description of the key",
    example = "This key is used on homepage. It's a label of sign up button.",
  )
  val description: String? = null,
  @Schema(description = "If key is pluralized. If it will be reflected in the editor")
  val isPlural: Boolean = false,
  @Schema(
    description =
      "The argument name for the plural. " +
        "If null, value will be guessed from the values provided in translations.",
  )
  val pluralArgName: String? = null,
) : WithRelatedKeysInOrder {
  @JsonSetter("namespace")
  fun setJsonNamespace(namespace: String?) {
    this.namespace = getSafeNamespace(namespace)
  }
}
