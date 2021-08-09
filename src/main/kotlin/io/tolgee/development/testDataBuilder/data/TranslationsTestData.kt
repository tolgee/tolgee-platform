package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.DataBuilders
import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.model.translation.Translation

class TranslationsTestData {
  var project: Project
  lateinit var englishLanguage: Language
  lateinit var germanLanguage: Language
  var user: UserAccount
  lateinit var aKey: Key
  lateinit var projectBuilder: DataBuilders.ProjectBuilder
  lateinit var aKeyGermanTranslation: Translation

  val root: TestDataBuilder = TestDataBuilder().apply {
    user = addUserAccount {
      self {
        username = "franta"
      }
    }.self
    project = addProject {
      self {
        name = "Franta's project"
        userOwner = user
      }

      addPermission {
        self {
          project = this@addProject.self
          user = this@TranslationsTestData.user
          type = Permission.ProjectPermissionType.MANAGE
        }
      }

      englishLanguage = addLanguage {
        self {
          name = "English"
          tag = "en"
          originalName = "English"
        }
      }.self
      germanLanguage = addLanguage {
        self {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }
      }.self

      aKey = addKey {
        self.name = "A key"
        aKeyGermanTranslation = addTranslation {
          self {
            key = this@addKey.self
            language = germanLanguage
            text = "Z translation"
            state = TranslationState.REVIEWED
          }
          addMeta {
            self {
              tags.add(
                Tag().apply {
                  this.project = this@addProject.self
                  name = "Cool tag"
                }
              )
            }
          }
        }.self
      }.self

      addKey {
        self.name = "Z key"
        addTranslation {
          self {
            key = this@addKey.self
            language = englishLanguage
            text = "A translation"
          }
        }
      }
      projectBuilder = this
    }.self
  }

  fun addKeyWithDot() {
    projectBuilder.addKey {
      self {
        name = "key.with.dots"
      }
    }
  }

  fun addKeysWithScreenshots() {
    projectBuilder.addKey {
      self {
        name = "key with screenshot"
      }
      addScreenshot {}
      addScreenshot {}
    }
    projectBuilder.addKey {
      self {
        name = "key with screenshot 2"
      }
      addScreenshot {}
      addScreenshot {}
    }
  }

  fun generateLotOfData() {
    root.data.projects[0].apply {
      (1..99).forEach {
        val padNum = it.toString().padStart(2, '0')
        addKey {
          self { name = "key $padNum" }
          addTranslation {
            self {
              key = this@addKey.self
              language = germanLanguage
              text = "I am key $padNum's german translation."
            }
          }
          addTranslation {
            self {
              key = this@addKey.self
              language = englishLanguage
              text = "I am key $padNum's english translation."
            }
          }
        }
      }
    }
  }

  fun addFewKeysWithTags() {
    root.data.projects[0].apply {
      addKey {
        self {
          name = "Key with tag"
          addMeta {
            self.tags.add(
              Tag().apply {
                name = "Cool tag"
                project = root.data.projects[0].self
              }
            )
          }
        }
      }
      addKey {
        self {
          name = "Another key with tag"
          addMeta {
            self.tags.add(
              Tag().apply {
                name = "Another cool tag"
                project = root.data.projects[0].self
              }
            )
          }
        }
      }
      addKey {
        self {
          name = "Key with tag 2"
          addMeta {
            self.tags.add(
              Tag().apply {
                name = "Cool tag"
                project = root.data.projects[0].self
              }
            )
          }
        }
      }
    }
  }

  fun generateCursorSearchData() {
    root.data.projects[0].apply {
      addKey {
        self { name = "Hello" }
      }
      addKey {
        self { name = "Hello 2" }
      }
      addKey {
        self { name = "Hello 3" }
      }
      addKey {
        self { name = "Hi" }
      }
      addKey {
        self { name = "Hi 2" }
      }
    }
  }

  fun generateCursorTestData() {
    root.data.projects[0].apply {
      addKey {
        self { name = "a" }
        addTranslation {
          self {
            key = this@addKey.self
            language = germanLanguage
            text = "d"
          }
        }
      }
      addKey {
        self { name = "b" }
        addTranslation {
          self {
            key = this@addKey.self
            language = germanLanguage
            text = "d"
          }
        }
      }
      addKey {
        self { name = "c" }
        addTranslation {
          self {
            key = this@addKey.self
            language = germanLanguage
            text = "d"
          }
        }
      }
      addKey {
        self { name = "d" }
        addTranslation {
          self {
            key = this@addKey.self
            language = germanLanguage
            text = "a"
          }
        }
      }
      addKey {
        self { name = "e" }
        addTranslation {
          self {
            key = this@addKey.self
            language = germanLanguage
            text = "a"
          }
        }
      }
      addKey {
        self { name = "f" }
        addTranslation {
          self {
            key = this@addKey.self
            language = germanLanguage
            text = "a"
          }
        }
      }
    }
  }
}
