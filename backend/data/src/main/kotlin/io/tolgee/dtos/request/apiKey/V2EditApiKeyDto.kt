package io.tolgee.dtos.request.apiKey

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ApiScope
import javax.validation.constraints.NotEmpty

data class V2EditApiKeyDto(
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
    return scopes.map { obj: ApiScope -> obj.value }.toSet()
  }
}
