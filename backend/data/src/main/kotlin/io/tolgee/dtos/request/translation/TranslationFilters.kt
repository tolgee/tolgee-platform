package io.tolgee.dtos.request.translation

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject

open class TranslationFilters(
  @field:Parameter(
    description = """Translation state in the format: languageTag,state. You can use this parameter multiple times.

When used with multiple states for same language it is applied with logical OR. 
 
When used with multiple languages, it is applied with logical AND.
    """,
    examples = [
      ExampleObject("en,TRANSLATED"),
      ExampleObject("en-US,UNTRANSLATED"),
      ExampleObject("fr,REVIEWED"),
    ]
  )
  open val filterState: List<String>? = null
) {
  @field:Parameter(
    description = """Languages to be contained in response.
                
To add multiple languages, repeat this param (eg. ?languages=en&languages=de)""",
    example = "en"
  )
  var languages: Set<String>? = null

  @field:Parameter(description = "String to search in key name or translation text")
  var search: String? = null

  @field:Parameter(description = "Selects only one key with provided name")
  var filterKeyName: String? = null

  @field:Parameter(description = "Selects only one key with provided id")
  var filterKeyId: List<Long>? = null

  @field:Parameter(description = "Selects only keys, where translation is missing in any language")
  var filterUntranslatedAny: Boolean = false

  @field:Parameter(description = "Selects only keys, where translation is provided in any language")
  var filterTranslatedAny: Boolean = false

  @field:Parameter(
    description = "Selects only keys, where translation is missing in specified language",
    example = "en-US"
  )
  var filterUntranslatedInLang: String? = null

  @field:Parameter(
    description = "Selects only keys, where translation is provided in specified language",
    example = "en-US"
  )
  var filterTranslatedInLang: String? = null

  @field:Parameter(description = "Selects only keys with screenshots")
  var filterHasScreenshot: Boolean = false

  @field:Parameter(description = "Selects only keys without screenshots")
  var filterHasNoScreenshot: Boolean = false

  @field:Parameter(description = "Selects only keys with provided tag")
  var filterTag: List<String>? = null
}
