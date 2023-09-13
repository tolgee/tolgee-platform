package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.util.getSafeNamespace
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

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

  @Schema(description = "Tags of the key. If not provided tags won't be modified")
  val tags: List<String>? = null,

  @Schema(description = "IDs of screenshots to delete")
  val screenshotIdsToDelete: List<Long>? = null,

  @Schema(description = "Ids of screenshots uploaded with /v2/image-upload endpoint")
  @Deprecated("Use screenshotsToAdd instead")
  val screenshotUploadedImageIds: List<Long>? = null,

  val screenshotsToAdd: List<KeyScreenshotDto>? = null
) {
  @JsonSetter("namespace")
  fun setJsonNamespace(namespace: String?) {
    this.namespace = getSafeNamespace(namespace)
  }
}
