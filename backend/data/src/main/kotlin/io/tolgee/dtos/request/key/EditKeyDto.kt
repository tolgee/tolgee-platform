package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.ValidationConstants
import io.tolgee.util.getSafeNamespace
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class EditKeyDto(
  @field:NotBlank
  @field:Length(max = 2000)
  var name: String = "",

  @field:Length(max = ValidationConstants.MAX_NAMESPACE_LENGTH)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  var namespace: String? = null,
) {

  @JsonSetter("namespace")
  fun setJsonNamespace(namespace: String?) {
    this.namespace = getSafeNamespace(namespace)
  }
}
