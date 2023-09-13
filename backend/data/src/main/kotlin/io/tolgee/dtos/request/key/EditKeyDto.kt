package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.constants.ValidationConstants
import io.tolgee.util.getSafeNamespace
import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotBlank

data class EditKeyDto(
  @field:NotBlank
  @field:Length(max = 2000)
  var name: String = "",

  @field:Length(max = ValidationConstants.MAX_NAMESPACE_LENGTH)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  var namespace: String? = null,
) {
  @JsonSetter("namespace")
  fun setJsonNamespace(namespace: String?) {
    this.namespace = getSafeNamespace(namespace)
  }
}
