package io.tolgee.dtos.request.key

import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class EditKeyDto(
  @field:NotBlank
  @field:Length(max = 2000)
  var name: String = "",

  @field:Length(max = 100)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  var namespace: String? = null,
) {
  @JsonSetter("namespace")
  fun setJsonNamespace(namespace: String?) {
    if (namespace == "") {
      this.namespace = null
      return
    }
    this.namespace = namespace
  }
}
