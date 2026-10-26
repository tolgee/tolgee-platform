package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.CommunityActivityReplayer
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.model.enums.TranslationSuggestionState
import jakarta.persistence.EntityManager
import java.util.Date

open class CommunityStatsTestData : BaseTestData(OWNER_USERNAME, "Community Stats Private") {
  lateinit var publicProject: Project
  lateinit var publicProjectBuilder: ProjectBuilder
  lateinit var czech: Language
  lateinit var german: Language
  lateinit var publicKeys: List<KeyBuilder>
  val privateProject: Project get() = project

  val alice: UserAccount =
    root
      .addUserAccountWithoutOrganization {
        username = "alice@community.test"
        name = "Alice Adams"
      }.self
  val bob: UserAccount =
    root
      .addUserAccountWithoutOrganization {
        username = "bob@community.test"
        name = "Bob Brown"
      }.self
  val carol: UserAccount =
    root
      .addUserAccountWithoutOrganization {
        username = "carol@community.test"
        name = "Carol Clark"
      }.self
  val dave: UserAccount =
    root
      .addUserAccountWithoutOrganization {
        username = "dave@community.test"
        name = "Dave Disabled"
        disabledAt = Date()
      }.self

  init {
    root.apply {
      publicProjectBuilder =
        addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
          name = PUBLIC_PROJECT_NAME
          public = true
          suggestionsMode = SuggestionsMode.ENABLED
        }.build {
          val en =
            addLanguage {
              name = "English"
              tag = "en"
              originalName = "English"
            }.self
          self.baseLanguage = en
          czech =
            addLanguage {
              name = "Czech"
              tag = "cs"
              originalName = "čeština"
              flagEmoji = "🇨🇿"
            }.self
          german =
            addLanguage {
              name = "German"
              tag = "de"
              originalName = "Deutsch"
              flagEmoji = "🇩🇪"
            }.self
          publicKeys = (1..3).map { i -> addKey { name = "key-$i" } }
          publicKeys[0]
            .addTranslation {
              language = czech
              text = "Překlad key-1"
            }.build {
              addComment {
                text = "Looks right"
                author = alice
              }
            }
          suggest(publicKeys[0], alice, czech, TranslationSuggestionState.ACCEPTED)
          suggest(publicKeys[1], alice, czech, TranslationSuggestionState.ACCEPTED)
          suggest(publicKeys[2], alice, czech, TranslationSuggestionState.ACTIVE)
          suggest(publicKeys[0], alice, german, TranslationSuggestionState.DECLINED)
          suggest(publicKeys[0], bob, czech, TranslationSuggestionState.ACCEPTED)
          suggest(publicKeys[1], bob, czech, TranslationSuggestionState.ACTIVE)
          suggest(publicKeys[2], dave, czech, TranslationSuggestionState.ACCEPTED)
        }
      publicProject = publicProjectBuilder.self

      projectBuilder.apply {
        val privateCzech =
          addLanguage {
            name = "Czech"
            tag = "cs"
            originalName = "čeština"
          }.self
        val key = addKey { name = "private-key" }
        suggest(key, alice, privateCzech, TranslationSuggestionState.ACCEPTED)
        suggest(key, carol, privateCzech, TranslationSuggestionState.ACTIVE)
      }
    }
  }

  fun replayActivity(entityManager: EntityManager) {
    CommunityActivityReplayer.replay(entityManager, listOf(publicProject.id, privateProject.id), maintainerId = user.id)
  }

  protected fun suggest(
    key: KeyBuilder,
    author: UserAccount,
    language: Language,
    state: TranslationSuggestionState,
  ) {
    key.addSuggestion {
      this.author = author
      this.language = language
      this.state = state
      this.translation = "${language.tag} suggestion for ${key.self.name} by ${author.name}"
    }
  }

  companion object {
    const val PUBLIC_PROJECT_NAME = "Community Stats Demo"
    const val OWNER_USERNAME = "communityStatsOwner"
  }
}
