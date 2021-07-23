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
        self {
          name = "Untranslated key"
        }
      }
      addProject(user) {
        addPermission {
          self {
            user = this@ProjectsTestData.user
            type = Permission.ProjectPermissionType.MANAGE
          }
        }
        project2 = self
        self.name = "Project 2"
        project2English = addLanguage {
          self {
            name = "English"
            tag = "en"
          }
        }.self
        project2Deutsch = addLanguage {
          self {
            name = "Deutsch"
            tag = "de"
          }
        }.self
        addKey {
          self {
            name = "Untranslated"
          }
        }
        addKey {
          self {
            name = "Translated to both"
          }
          addTranslation {
            self {
              key = this@addKey.self
              text = "Translated"
              language = project2English
            }
          }
          addTranslation {
            self {
              key = this@addKey.self
              text = "Translated in de"
              language = project2Deutsch
            }
          }
        }
        addKey {
          self {
            name = "Machine translated to en"
          }
          addTranslation {
            self {
              key = this@addKey.self
              text = "Translated"
              state = TranslationState.MACHINE_TRANSLATED
              language = project2English
            }
          }
        }
        addKey {
          self {
            name = "Reviewed in de"
          }
          addTranslation {
            self {
              key = this@addKey.self
              text = "Reviewed"
              state = TranslationState.REVIEWED
              language = project2Deutsch
            }
          }
        }
        addKey {
          self {
            name = "Needs review in both"
          }
          addTranslation {
            self {
              key = this@addKey.self
              text = "Needs review in de"
              state = TranslationState.NEEDS_REVIEW
              language = project2Deutsch
            }
          }
          addTranslation {
            self {
              key = this@addKey.self
              text = "Needs review"
              state = TranslationState.NEEDS_REVIEW
              language = project2English
            }
          }
        }
      }
    }
  }
}
