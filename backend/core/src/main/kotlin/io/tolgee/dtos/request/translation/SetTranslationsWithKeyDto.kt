package io.tolgee.dtos.request.translation

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated

@Validated
data class SetTranslationsWithKeyDto(
  /**
   * Key full path is stored as name in entity
   */
  @Schema(description = "Key name to set translations for", example = "what_a_key_to_translate")
  @field:NotBlank
  @field:Length(max = 2000)
  val key: String = "",
  @field:Length(max = 100)
  @Schema(description = "The namespace of the key. (When empty or null default namespace will be used)")
  val namespace: String? = null,
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
    description = """List of languages to return translations for. 

If not provided, only modified translation will be provided.
    """,
    example = """["en", "de", "fr"]""",
  )
  val languagesToReturn: Set<String>? = null,
)
