package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.ValidationConstants
import io.tolgee.util.getSafeNamespace
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.Length

data class EditKeyDto(
  @field:NotBlank
  @field:Length(max = 2000)
  var name: String = "",
  @field:Length(max = ValidationConstants.MAX_NAMESPACE_LENGTH)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  var namespace: String? = null,
  @Size(max = 2000)
  @Schema(
    description = "Description of the key",
    example = "This key is used on homepage. It's a label of sign up button.",
  ) val description: String? = null,
) {
  @JsonSetter("namespace")
  fun setJsonNamespace(namespace: String?) {
    this.namespace = getSafeNamespace(namespace)
  }
}
