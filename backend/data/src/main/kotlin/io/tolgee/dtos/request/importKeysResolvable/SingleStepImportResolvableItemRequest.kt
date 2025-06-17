package io.tolgee.dtos.request.importKeysResolvable

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.request.key.KeyScreenshotDto
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length

class SingleStepImportResolvableItemRequest(
  /**
   * Key full path is stored as name in entity
   */
  @Schema(description = "Key name to set translations for", example = "what_a_key_to_translate")
  @field:NotBlank
  @field:Length(max = 2000)
  val name: String = "",
  @field:Length(max = 100)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  val namespace: String? = null,
  val screenshots: List<KeyScreenshotDto>? = null,
  /**
   * Map of language tag -> text
   */
  @field:NotNull
  @Schema(
    description = "Object mapping language tag to translation",
  )
  val translations: Map<String, SingleStepImportResolvableTranslationRequest?> = mapOf(),
)
