package io.tolgee.dtos.request.apiKey

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonSetter
import io.tolgee.model.enums.ApiScope
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.stream.Collectors

data class EditApiKeyDto(
  @field:NotNull
  var id: Long = 0,
  @field:NotEmpty
  var scopes: Set<ApiScope> = setOf(),
  var description: String? = null
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
