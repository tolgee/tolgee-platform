package io.tolgee.ee.data.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class CreateMultipleTranslationMemoryEntriesRequest {
  @Schema(example = "Hello world", description = "Source text (in the TM's source language)")
  @field:NotBlank
  @field:Size(max = TranslationMemoryEntry.MAX_TEXT_LENGTH)
  var sourceText: String = ""

  @Schema(description = "Target translations to create, one per target language")
  @field:NotEmpty
  @field:Valid
  var translations: List<CreateMultipleTranslationMemoryEntriesTranslationRequest> = emptyList()
}

class CreateMultipleTranslationMemoryEntriesTranslationRequest {
  @Schema(example = "de", description = "Target language tag according to BCP 47 definition")
  @field:NotBlank
  @field:Size(min = 1, max = 20)
  @field:Pattern(regexp = "^[^,]*$", message = "can not contain comma")
  var targetLanguageTag: String = ""

  @Schema(example = "Hallo Welt", description = "Target translation text")
  @field:NotBlank
  @field:Size(max = TranslationMemoryEntry.MAX_TEXT_LENGTH)
  var targetText: String = ""
}
