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
    ],
  )
  open val filterState: List<String>? = null,
) {
  @field:Parameter(
    description = """Languages to be contained in response.
                
To add multiple languages, repeat this param (eg. ?languages=en&languages=de)""",
    example = "en",
  )
  var languages: Set<String>? = null

  @field:Parameter(description = "String to search in key name or translation text")
  var search: String? = null

  @field:Parameter(description = "Selects key with provided names. Use this param multiple times to fetch more keys.")
  var filterKeyName: List<String>? = null

  @field:Parameter(description = "Selects key with provided ID. Use this param multiple times to fetch more keys.")
  var filterKeyId: List<Long>? = null

  @field:Parameter(
    description =
      "Selects only keys for which the translation is missing in any returned language. " +
        "It only filters for translations included in returned languages.",
  )
  var filterUntranslatedAny: Boolean = false

  @field:Parameter(description = "Selects only keys, where translation is provided in any language")
  var filterTranslatedAny: Boolean = false

  @field:Parameter(
    description =
      "Selects only keys where the translation is missing for the specified language. " +
        "The specified language must be included in the returned languages. Otherwise, this filter doesn't apply.",
    example = "en-US",
  )
  var filterUntranslatedInLang: String? = null

  @field:Parameter(
    description = "Selects only keys, where translation is provided in specified language",
    example = "en-US",
  )
  var filterTranslatedInLang: String? = null

  @field:Parameter(description = "Selects only keys with screenshots")
  var filterHasScreenshot: Boolean = false

  @field:Parameter(description = "Selects only keys without screenshots")
  var filterHasNoScreenshot: Boolean = false

  @field:Parameter(
    description = """Filter namespaces. 

To filter default namespace, set to empty string.
  """,
  )
  var filterNamespace: List<String>? = null

  @field:Parameter(description = "Selects only keys with provided tag")
  var filterTag: List<String>? = null

  @field:Parameter(
    description = "Selects only keys, where translation in provided langs is in outdated state",
    example = "en-US",
  )
  var filterOutdatedLanguage: List<String>? = null

  @field:Parameter(
    description = "Selects only keys, where translation in provided langs is not in outdated state",
    example = "en-US",
  )
  var filterNotOutdatedLanguage: List<String>? = null

  @field:Parameter(
    description = "Selects only key affected by activity with specidfied revision ID",
    example = "1234567",
  )
  var filterRevisionId: List<Long>? = null

  @field:Parameter(
    description = "Select only keys which were not successfully translated by batch job with provided id",
  )
  var filterFailedKeysOfJob: Long? = null

  @field:Parameter(
    description = "Select only keys which are in specified task",
  )
  var filterTaskNumber: List<Long>? = null
}
