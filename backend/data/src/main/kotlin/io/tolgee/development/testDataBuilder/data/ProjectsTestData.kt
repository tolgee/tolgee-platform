package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState

class ProjectsTestData : BaseTestData() {
  lateinit var project2English: Language
  lateinit var project2Deutsch: Language
  lateinit var project2: Project

  init {
    root.apply {
      projectBuilder.addKey {
        name = "Untranslated key"
      }
      addProject(user) {
        project2 = this
        name = "Project 2"
      }.build {
        addPermission {
          user = this@ProjectsTestData.user
          type = Permission.ProjectPermissionType.MANAGE
        }
        project2English = addLanguage {

          name = "English"
          tag = "en"
        }.self
        project2Deutsch = addLanguage {

          name = "Deutsch"
          tag = "de"
        }.self
        addKey {
          name = "Untranslated"
        }
        addKey {
          name = "Translated to both"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Translated"
            language = project2English
          }
          addTranslation {
            key = this@keyBuilder.self
            text = "Translated in de"
            language = project2Deutsch
          }
        }
        addKey {
          name = "Machine translated to en"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Translated"
            state = TranslationState.TRANSLATED
            auto = true
            language = project2English
          }
        }
        addKey {
          name = "Reviewed in de"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Reviewed"
            state = TranslationState.REVIEWED
            language = project2Deutsch
          }
        }
        addKey {
          name = "Needs review in both"
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            text = "Needs review in de"
            language = project2Deutsch
          }
          addTranslation {
            key = this@keyBuilder.self
            text = "Needs review"
            language = project2English
          }
        }
      }
    }
  }
}
