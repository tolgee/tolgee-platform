package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.ProfileVisibility
import io.tolgee.model.enums.TranslationSuggestionState

/** Dates are spread over past weeks and the activity is replayed afterwards by `CommunityStatsE2eDataController`. */
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
    root.addUserAccountWithoutOrganization {
      username = "henry.public@community.test"
      name = "Henry Public"
      profileVisibility = ProfileVisibility.PUBLIC
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
      var commentsAdded = 0
      publicKeys.drop(1).forEach { key ->
        listOf(german, french).forEachIndexed { translationIndex, language ->
          key
            .addTranslation {
              this.language = language
              text = "${language.tag} translation of ${key.self.name}"
            }.build {
              repeat(1 + translationIndex % 2) {
                addComment {
                  text = "Comment ${commentsAdded + 1}"
                  author = authors[commentsAdded % authors.size]
                }
                commentsAdded++
              }
            }
        }
      }
    }
    publicKeys[2].addTranslation {
      language = czech
      text = "Překlad key-3"
    }
    projectBuilder.apply {
      data.keys.single { it.self.name == "private-key" }.addTranslation {
        language = data.languages.single { it.self.tag == "cs" }.self
        text = "Překlad private-key"
      }
    }
  }
}
