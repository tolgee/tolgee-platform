package io.tolgee.dtos.request.translation

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length

class ImportKeysItemDto(
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
  @Schema(
    description = "Description of key",
    example = "This key is used on homepage. It's a label of sign up button.",
  )
  val description: String?,
  /**
   * Map of language tag -> text
   */
  @field:NotNull
  @Schema(
    description = "Object mapping language tag to translation",
    example = "{\"en\": \"What a translated value!\", \"cs\": \"Jaká to přeložená hodnota!\"}",
  )
  val translations: Map<String, String?> = mapOf(),
  @Schema(
    description = "Tags of the key",
    example = "[\"homepage\", \"user-profile\"]",
  )
  val tags: List<String>? = null,
)
