package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Tag
import io.tolgee.model.translation.Translation

class TranslationNsAndTagsData {
  lateinit var englishLanguage: Language
  lateinit var czechLanguage: Language
  lateinit var user: UserAccount
  lateinit var projectBuilder: ProjectBuilder
  lateinit var translation: Translation
  lateinit var userAccountBuilder: UserAccountBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val userAccountBuilder =
        addUserAccount {
          username = "olin"
          user = this
        }

      addProject {
        name = "Test project"
        organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
      }.build {

        val newProject = this.self
        englishLanguage =
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
          }.self

        czechLanguage =
          addLanguage {
            name = "Czech"
            tag = "cs"
            originalName = "Čeština"
          }.self

        self.baseLanguage = englishLanguage

        for (i in 1..20) {
          val paddedNum = i.toString().padStart(2, '0')
          addKey {
            name = "Key $paddedNum"
          }.build {
            addTranslation {
              language = englishLanguage
              text = "Translation $paddedNum"
              state = TranslationState.REVIEWED
            }

            addTranslation {
              language = czechLanguage
              text = "Překlad $paddedNum"
              state = TranslationState.REVIEWED
            }
            newProject.useNamespaces = true
            setNamespace("Namespace $paddedNum")
            addMeta {
              self {
                tags.add(
                  Tag().apply {
                    project = newProject
                    name = "Tag $paddedNum"
                  },
                )
              }
            }
          }
        }

        projectBuilder = this
      }.self
    }
}
