package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationSuggestionState

class CommunityContributionDayTestData : BaseTestData("counters_author", "counters_project") {
  val keyBuilder = projectBuilder.addKey { name = "key" }
  val key get() = keyBuilder.self
  val german: Language =
    projectBuilder
      .addLanguage {
        name = "German"
        tag = "de"
      }.self
  val suggester: UserAccount = root.addUserAccountWithoutOrganization { username = "counters_suggester" }.self
  val otherSuggester: UserAccount =
    root
      .addUserAccountWithoutOrganization { username = "counters_other_suggester" }
      .self
  val suggestion: TranslationSuggestion = suggest(suggester, TranslationSuggestionState.ACTIVE)
  val otherSuggestion: TranslationSuggestion = suggest(otherSuggester, TranslationSuggestionState.ACTIVE)
  val declinedSuggestion: TranslationSuggestion = suggest(suggester, TranslationSuggestionState.DECLINED)

  private fun suggest(
    author: UserAccount,
    state: TranslationSuggestionState,
  ): TranslationSuggestion =
    keyBuilder
      .addSuggestion {
        this.author = author
        this.language = englishLanguage
        this.state = state
        this.translation = "suggestion by ${author.username}"
      }.self
}
