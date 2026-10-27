package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationSuggestionState

class McpSuggestionTestData {
  val base = SuggestionsTestData(SuggestionsMode.ENABLED)
  val ownerPat: Pat
  val czechReviewerPat: Pat
  val suggestOnlyApiKey: ApiKey

  init {
    ownerPat = base.userAccountBuilder.addPat { description = "owner" }.self
    czechReviewerPat = base.czechReviewer.addPat { description = "cs reviewer" }.self
    suggestOnlyApiKey =
      base.projectBuilder
        .addApiKey {
          key = "suggest-only-api-key"
          scopesEnum = mutableSetOf(Scope.TRANSLATIONS_VIEW, Scope.TRANSLATIONS_SUGGEST)
          userAccount = base.user
        }.self
    base.keys[1].addSuggestion {
      this.language = base.czechLanguage
      this.author = base.projectTranslator.self
      this.translation = "Declined suggestion 1-1"
      this.state = TranslationSuggestionState.DECLINED
    }
  }
}
