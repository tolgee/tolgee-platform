package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.model.translation.Translation

class TranslationsTestData {
  lateinit var project: Project
  lateinit var englishLanguage: Language
  lateinit var germanLanguage: Language
  var user: UserAccount
  lateinit var aKey: Key
  lateinit var projectBuilder: ProjectBuilder
  lateinit var aKeyGermanTranslation: Translation

  val root: TestDataBuilder = TestDataBuilder().apply {
    user = addUserAccount {
      username = "franta"
    }.self
    addProject {
      name = "Franta's project"
      userOwner = user
      project = this
    }.build project@{
      addPermission {
        user = this@TranslationsTestData.user
        type = Permission.ProjectPermissionType.MANAGE
      }
      englishLanguage = addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
      }.self
      germanLanguage = addLanguage {
        name = "German"
        tag = "de"
        originalName = "Deutsch"
      }.self

      addKey {
        name = "A key"
        aKey = this
      }.build {
        addTranslation {
          language = germanLanguage
          text = "Z translation"
          state = TranslationState.REVIEWED
          auto = true
          mtProvider = MtServiceType.GOOGLE
          aKeyGermanTranslation = this
        }.build {
          addMeta {
            tags.add(
              Tag().apply {
                this.project = this@project.self
                name = "Cool tag"
              }
            )
          }
          addComment {
            text = "Comment"
          }
        }
      }

      val zKeyBuilder = addKey {
        name = "Z key"
      }
      zKeyBuilder.build {
        addTranslation {
          key = zKeyBuilder.self
          language = englishLanguage
          text = "A translation"
          auto = true
        }.build {
          addMeta {
            self {
              tags.add(
                Tag().apply {
                  this.project = this@project.self
                  name = "Lame tag"
                }
              )
            }
          }
        }
      }
      projectBuilder = this
    }.self
  }

  fun addTranslationsWithStates() {
    projectBuilder.apply {
      addKey {
        name = "state test key"
      }.build {
        addTranslation {
          language = germanLanguage
          text = "a"
          state = TranslationState.REVIEWED
        }
        addTranslation {
          language = englishLanguage
          text = "aa"
          state = TranslationState.REVIEWED
        }
      }
      addKey {
        name = "state test key 2"
      }.build {
        addTranslation {
          language = germanLanguage
          text = "a"
          state = TranslationState.TRANSLATED
        }
        addTranslation {
          language = englishLanguage
          text = "aa"
          state = TranslationState.REVIEWED
        }
      }
      addKey {
        name = "state test key 3"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "aa"
          state = TranslationState.REVIEWED
        }
      }
      addKey {
        name = "state test key 4"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "aa"
        }
      }
      addKey {
        name = "state test key 5"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "aa"
          auto = true
        }
      }
    }
  }

  fun addKeyWithDot() {
    projectBuilder.addKey {
      name = "key.with.dots"
    }
  }

  fun addKeysWithScreenshots() {
    projectBuilder.addKey {
      name = "key with screenshot"
    }.build {
      addScreenshot {}
      addScreenshot {}
    }
    projectBuilder.addKey {
      name = "key with screenshot 2"
    }.build {
      addScreenshot {}
      addScreenshot {}
    }
  }

  fun generateLotOfData(count: Long = 99) {
    root.data.projects[0].apply {
      (1..count).forEach {
        val padNum = it.toString().padStart(2, '0')
        addKey { name = "key $padNum" }.build {
          addTranslation {
            language = germanLanguage
            text = "I am key $padNum's german translation."
          }
          addTranslation {
            language = englishLanguage
            text = "I am key $padNum's english translation."
          }
        }
      }
    }
  }

  fun addFewKeysWithTags() {
    root.data.projects[0].apply {
      addKey {
        name = "Key with tag"
      }.build {
        addMeta {
          tags.add(
            Tag().apply {
              name = "Cool tag"
              project = root.data.projects[0].self
            }
          )
        }
      }
      addKey {
        name = "Another key with tag"
      }.build {
        addMeta {
          tags.add(
            Tag().apply {
              name = "Another cool tag"
              project = root.data.projects[0].self
            }
          )
        }
      }
      addKey {
        name = "Key with tag 2"
      }.build {
        addMeta {
          tags.add(
            Tag().apply {
              name = "Cool tag"
              project = root.data.projects[0].self
            }
          )
        }
      }
    }
  }

  fun generateCursorSearchData() {
    root.data.projects[0].apply {
      addKey {
        name = "Hello"
      }
      addKey {
        name = "Hello 2"
      }
      addKey {
        name = "Hello 3"
      }
      addKey {
        name = "Hi"
      }
      addKey {
        name = "Hi 2"
      }
    }
  }

  fun generateCursorTestData() {
    root.data.projects[0].apply {
      addKey { name = "a" }.build {
        addTranslation {
          language = germanLanguage
          text = "f"
        }
      }
      addKey { name = "b" }.build {
        addTranslation {
          language = germanLanguage
          text = "f"
        }
      }
      addKey { name = "c" }.build {
        addTranslation {
          language = germanLanguage
          text = "d"
        }
      }
      addKey { name = "d" }.build {
        addTranslation {
          language = germanLanguage
          text = "b"
        }
      }
      addKey { name = "e" }.build {
        addTranslation {
          language = germanLanguage
          text = "b"
        }
      }
      addKey { name = "f" }.build {
        addTranslation {
          language = germanLanguage
          text = "a"
        }
      }
    }
  }

  fun generateCommentTestData() {
    root.data.projects[0].apply {
      addKey { name = "ee" }.build {
        addTranslation {
          language = germanLanguage
          text = "d"
        }.build {
          (1..5).forEach {
            addComment {
              text = "Comment $it"
            }
          }
        }
        addTranslation {
          language = englishLanguage
          text = "d"
        }
          .build {
            (1..3).forEach {
              addComment {
                text = "Comment $it"
              }
            }
          }
      }
    }
  }

  fun addUntranslated() {
    return projectBuilder.run {
      addKey {
        name = "lala"
      }.build {
        addTranslation {
          text = null
          language = englishLanguage
          state = TranslationState.UNTRANSLATED
        }.self
      }
    }
  }

  fun generateScopedData() {
    projectBuilder.run {
      addKey {
        name = "hello.i.am.scoped"
      }.build {
        addTranslation {
          text = "yupee!"
          language = englishLanguage
        }
      }
    }
  }

  fun addCommentStatesData() {
    projectBuilder.run {
      addKey {
        name = "commented_key"
      }.build {
        addTranslation {
          language = germanLanguage
          text = "Nice"
        }.build {
          addComment {
            text = "aaaa"
            state = TranslationCommentState.RESOLVED
          }
          addComment {
            text = "aaaa"
            state = TranslationCommentState.RESOLVED
          }
          addComment {
            text = "aaaa"
            state = TranslationCommentState.NEEDS_RESOLUTION
          }
          addComment {
            text = "aaaa"
            state = TranslationCommentState.NEEDS_RESOLUTION
          }
        }
      }
    }
  }
}
