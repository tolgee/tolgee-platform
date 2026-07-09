package io.tolgee.dtos.request.translation

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.tolgee.service.queryBuilders.translationViewBuilder.WildcardLikeUtil

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
  var filterUntranslatedAny: Boolean? = false

  @field:Parameter(description = "Selects only keys, where translation is provided in any language")
  var filterTranslatedAny: Boolean? = false

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

  @field:Parameter(
    description = "Selects only keys, where translation was auto translated for specified languages.",
    example = "en-US",
  )
  var filterAutoTranslatedInLang: List<String>? = null

  @field:Parameter(description = "Selects only keys with screenshots")
  var filterHasScreenshot: Boolean? = false

  @field:Parameter(description = "Selects only keys without screenshots")
  var filterHasNoScreenshot: Boolean? = false

  @field:Parameter(description = "Selects only keys with a description")
  var filterHasDescription: Boolean? = false

  @field:Parameter(description = "Selects only keys without a description")
  var filterHasNoDescription: Boolean? = false

  @field:Parameter(
    description = """Selects only keys with provided namespaces. 

To filter default namespace, set to empty string.
  """,
  )
  var filterNamespace: List<String>? = null

  @field:Parameter(
    description = """Selects only keys without provided namespaces. 

To filter default namespace, set to empty string.
  """,
  )
  var filterNoNamespace: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with name matching the provided pattern.

$PATTERN_GRAMMAR_DOC""",
    examples = [ExampleObject("cart*"), ExampleObject("*_title"), ExampleObject("checkout")],
  )
  var filterKeyPattern: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with name not matching the provided pattern.

$PATTERN_GRAMMAR_DOC""",
  )
  var filterNoKeyPattern: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with description matching the provided pattern.
Keys without a description never match.

$PATTERN_GRAMMAR_DOC""",
  )
  var filterDescriptionPattern: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with description not matching the provided pattern.
Keys without a description always match.

$PATTERN_GRAMMAR_DOC""",
  )
  var filterNoDescriptionPattern: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with namespace matching the provided pattern.
Keys in the default namespace never match.

$PATTERN_GRAMMAR_DOC""",
  )
  var filterNamespacePattern: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with namespace not matching the provided pattern.
Keys in the default namespace always match.

$PATTERN_GRAMMAR_DOC""",
  )
  var filterNoNamespacePattern: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with a translation text matching the provided pattern,
in the format: languageTag,pattern. Use `*` as the language tag to match any of the returned languages.
The language tag is matched case-insensitively and must be included in the returned languages,
otherwise the request fails with 400.

$PATTERN_GRAMMAR_DOC""",
    examples = [ExampleObject("de,Warenkorb"), ExampleObject("*,cart*")],
  )
  var filterTranslationPattern: List<String>? = null

  @field:Parameter(
    description = """Selects only keys with no translation text matching the provided pattern,
in the format: languageTag,pattern. Use `*` as the language tag to match against any of the returned
languages. Keys with no translation in the specified language always match.
The language tag is matched case-insensitively and must be included in the returned languages,
otherwise the request fails with 400.

$PATTERN_GRAMMAR_DOC""",
  )
  var filterNoTranslationPattern: List<String>? = null

  @field:Parameter(description = "Selects only keys with provided tag")
  var filterTag: List<String>? = null

  @field:Parameter(description = "Selects only keys without provided tag")
  var filterNoTag: List<String>? = null

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

  @field:Parameter(
    description = "Filter task keys which are `not done`",
  )
  var filterTaskKeysNotDone: Boolean? = null

  @field:Parameter(
    description = "Filter task keys which are `done`",
  )
  var filterTaskKeysDone: Boolean? = null

  @field:Parameter(
    description = "Filter keys with unresolved comments in lang",
  )
  var filterHasUnresolvedCommentsInLang: List<String>? = null

  @field:Parameter(
    description = "Filter keys with any comments in lang",
  )
  var filterHasCommentsInLang: List<String>? = null

  @field:Parameter(
    description = "Filter key translations with labels",
    example = "labelId1,labelId2",
  )
  var filterLabel: List<String>? = null

  @field:Parameter(
    description = "Filter keys with open QA issues in lang",
  )
  var filterHasQaIssuesInLang: List<String>? = null

  @field:Parameter(
    description = """Filter keys with specific QA check type issues in the format: languageTag,checkType.
You can use this parameter multiple times.

A key matches if any of the selected check types is present in any of the selected languages.
    """,
    examples = [
      ExampleObject("en,PUNCTUATION_MISMATCH"),
      ExampleObject("fr,SPACES_MISMATCH"),
      ExampleObject("de,EMPTY_TRANSLATION"),
    ],
  )
  var filterQaCheckType: List<String>? = null

  @field:Parameter(
    description =
      "Filter keys whose QA checks are stale (pending recomputation) in lang. " +
        "When set, only keys with at least one stale translation in any of the provided " +
        "languages are returned.",
  )
  var filterQaChecksStaleInLang: List<String>? = null

  @field:Parameter(
    description = "Filter keys with any suggestions in lang",
  )
  var filterHasSuggestionsInLang: List<String>? = null

  @field:Parameter(
    description = "Filter keys with no suggestions in lang",
  )
  var filterHasNoSuggestionsInLang: List<String>? = null

  @field:Parameter(
    description = "Selects only keys from specified branch",
  )
  var branch: String? = null

  @field:Parameter(description = "Filter trashed keys by who deleted them (user IDs)")
  var filterDeletedByUserId: List<Long>? = null

  @field:Parameter(description = "If true, return only soft-deleted keys", hidden = true)
  var trashed: Boolean = false

  companion object {
    const val PATTERN_GRAMMAR_DOC = """Pattern syntax: `*` matches any sequence of characters
(`cart*` = starts with, `*_title` = ends with). A pattern without `*` matches anywhere in the value.
Matching is case-insensitive. `%` and `_` are matched literally.
You can use this parameter multiple times; all patterns must match (logical AND).
Limits: a pattern must not be empty, may be at most ${WildcardLikeUtil.MAX_PATTERN_LENGTH} characters long
with at most ${WildcardLikeUtil.MAX_WILDCARDS} wildcards, and at most ${WildcardLikeUtil.MAX_PATTERNS_PER_PARAM}
patterns may be provided per parameter; violations fail with 400."""

    /**
     * Params carrying free text where a comma is data, not a separator. Controllers bind
     * these verbatim via TranslationFiltersBindingCustomizer — a new pattern param must be
     * added here, otherwise Spring comma-splits its single values.
     */
    val VERBATIM_LIST_PARAMS =
      listOf(
        TranslationFilters::filterKeyName.name,
        TranslationFilters::filterKeyPattern.name,
        TranslationFilters::filterNoKeyPattern.name,
        TranslationFilters::filterDescriptionPattern.name,
        TranslationFilters::filterNoDescriptionPattern.name,
        TranslationFilters::filterNamespacePattern.name,
        TranslationFilters::filterNoNamespacePattern.name,
        TranslationFilters::filterTranslationPattern.name,
        TranslationFilters::filterNoTranslationPattern.name,
      )
  }
}
