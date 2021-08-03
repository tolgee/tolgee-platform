package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject

data class GetTranslationsParams(
  @field:Parameter(
    description = """Languages to be contained in response.
                
To add multiple languages, repeat this param (eg. ?languages=en&languages=de)""",
    example = "en"
  )
  val languages: Set<String>? = null,
  @field:Parameter(description = "String to search in key name or translation text")
  val search: String? = null,
  @field:Parameter(description = "Selects only one key with provided name")
  val filterKeyName: String? = null,
  @field:Parameter(description = "Selects only one key with provided id")
  val filterKeyId: Long? = null,
  @field:Parameter(description = "Selects only keys, where translation is missing in any language")
  val filterUntranslatedAny: Boolean = false,
  @field:Parameter(description = "Selects only keys, where translation is provided in any language")
  val filterTranslatedAny: Boolean = false,
  @field:Parameter(
    description = "Selects only keys, where translation is missing in specified language",
    example = "en-US"
  )
  val filterUntranslatedInLang: String? = null,
  @field:Parameter(
    description = "Selects only keys, where translation is provided in specified language",
    example = "en-US"
  )
  val filterTranslatedInLang: String? = null,
  @field:Parameter(description = "Selects only keys with screenshots")
  val filterHasScreenshot: Boolean = false,
  @field:Parameter(description = "Selects only keys without screenshots")
  val filterHasNoScreenshot: Boolean = false,
  @field:Parameter(
    description = "Translation state in format \"languageTag,state\"",
    examples = [
      ExampleObject("en,TRANSLATED"),
      ExampleObject("en-US,UNTRANSLATED"),
      ExampleObject("de,NEEDS_REVIEW"),
      ExampleObject("fr,MACHINE_TRANSLATED"),
    ]
  )
  val filterState: String? = null,

  @field:Parameter(description = "Cursor to get next data")
  val cursor: String? = null
)
