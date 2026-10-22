package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.TranslationSuggestionState

/** Dates are spread over past weeks afterwards by `CommunityStatsE2eDataController`. */
class CommunityStatsE2eData : CommunityStatsTestData() {
  init {
    val extraContributors =
      listOf("Dana Diaz", "Eli Evans", "Fatima Farouk", "Gus Garcia").map { name ->
        root
          .addUserAccountWithoutOrganization {
            username = name.lowercase().replace(' ', '.') + "@community.test"
            this.name = name
          }.self
      }
    publicProjectBuilder.apply {
      val french =
        addLanguage {
          name = "French"
          tag = "fr"
          originalName = "Français"
          flagEmoji = "🇫🇷"
        }.self
      val languages = listOf(czech, german, french)
      val keys = publicKeys + (4..12).map { i -> addKey { name = "key-$i" } }
      val authors = listOf(alice, bob) + extraContributors
      var i = 0
      authors.forEachIndexed { a, author ->
        languages.forEachIndexed { l, language ->
          val favouredLanguage = (a + l) % 3 == 0
          val count =
            when {
              favouredLanguage -> 7
              else -> 2
            }
          repeat(count) {
            val state =
              when (i % 5) {
                0 -> TranslationSuggestionState.DECLINED
                1, 2 -> TranslationSuggestionState.ACTIVE
                else -> TranslationSuggestionState.ACCEPTED
              }
            suggest(keys[i % keys.size], author, language, state)
            i++
          }
        }
      }
    }
  }
}
