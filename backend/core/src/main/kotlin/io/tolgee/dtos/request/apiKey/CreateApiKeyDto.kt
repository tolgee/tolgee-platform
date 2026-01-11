package io.tolgee.dtos.request.apiKey

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSetter
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.Scope
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.util.stream.Collectors

data class CreateApiKeyDto(
  @field:NotNull
  @field:Min(1)
  var projectId: Long = 0,
  @field:NotEmpty
  @JsonIgnore
  @Schema(
    example = """
    ["screenshots.upload", "screenshots.delete", "translations.edit", "screenshots.view", "translations.view", "keys.edit"]
    """,
  )
  var scopes: Set<Scope> = setOf(),
  @Schema(description = "Description of the project API key")
  @field:Length(max = 250, min = 1)
  var description: String? = null,
  @Schema(
    description =
      "Expiration date in epoch format (milliseconds)." +
        " When null key never expires.",
    example = "1661172869000",
  )
  val expiresAt: Long? = null,
) {
  @Suppress("unused")
  @JsonSetter("scopes")
  fun jsonSetScopes(scopes: Set<String>) {
    this.scopes = scopes.map { value -> Scope.fromValue(value) }.toSet()
  }

  @Suppress("unused")
  @JsonGetter("scopes")
  fun jsonGetScopes(): Set<String> {
    return scopes.stream().map { obj: Scope -> obj.value }.collect(Collectors.toSet())
  }
}
