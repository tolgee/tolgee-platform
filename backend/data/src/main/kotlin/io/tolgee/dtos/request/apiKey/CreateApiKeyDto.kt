package io.tolgee.dtos.request.apiKey

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ApiScope
import java.util.stream.Collectors
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

data class CreateApiKeyDto(
  @field:NotNull
  @field:Min(1)
  var projectId: Long = 0,

  @field:NotEmpty
  @JsonIgnore
  @Schema(
    example = """
    ["screenshots.upload", "screenshots.delete", "translations.edit", "screenshots.view", "translations.view", "keys.edit"]
    """
  )
  var scopes: Set<ApiScope> = setOf()
) {

  @Suppress("unused")
  @JsonSetter("scopes")
  fun jsonSetScopes(scopes: Set<String>) {
    this.scopes = scopes.map { value -> ApiScope.fromValue(value) }.toSet()
  }

  @Suppress("unused")
  @JsonGetter("scopes")
  fun jsonGetScopes(): Set<String> {
    return scopes.stream().map { obj: ApiScope -> obj.value }.collect(Collectors.toSet())
  }
}
