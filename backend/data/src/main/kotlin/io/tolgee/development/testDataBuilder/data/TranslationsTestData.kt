package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyInScreenshotPosition
import io.tolgee.model.translation.Translation

class TranslationsTestData {
  lateinit var project: Project
  lateinit var englishLanguage: Language
  lateinit var germanLanguage: Language
  lateinit var user: UserAccount
  lateinit var aKey: Key
  lateinit var projectBuilder: ProjectBuilder
  lateinit var aKeyGermanTranslation: Translation
  lateinit var keysOnlyUser: UserAccount

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val userAccountBuilder =
        addUserAccount {
          username = "franta"
          user = this
        }
      addProject {
        name = "Franta's project"
        organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        project = this
      }.build project@{
        addPermission {
          user = this@TranslationsTestData.user
          type = ProjectPermissionType.MANAGE
        }
        englishLanguage =
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            this@project.self.baseLanguage = this
          }.self
        germanLanguage =
          addLanguage {
            name = "German"
            tag = "de"
            originalName = "Deutsch"
          }.self

        addKey {
          name = "A key"
          aKey = this
        }.build {
          setDescription("A key description")
          addTranslation {
            language = germanLanguage
            text = "Z translation"
            state = TranslationState.REVIEWED
            auto = true
            outdated = true
            mtProvider = MtServiceType.GOOGLE
            aKeyGermanTranslation = this
          }.build {
            addComment {
              author = user
              text = "Comment"
              state = TranslationCommentState.RESOLVED
            }
          }
          addTag("Cool tag")
        }

        val zKeyBuilder =
          addKey {
            name = "Z key"
          }
        zKeyBuilder.build {
          addTranslation {
            key = zKeyBuilder.self
            language = englishLanguage
            text = "A translation"
            auto = true
          }.build {
            addTag("Lame tag")
            addTag("Some other tag")
          }
        }
        projectBuilder = this
      }.self
    }

  fun addKeysViewOnlyUser() {
    root.apply {
      addUserAccountWithoutOrganization {
        username = "pepa"
        keysOnlyUser = this
      }
    }

    projectBuilder.build {
      addPermission {
        user = this@TranslationsTestData.keysOnlyUser
        type = null
        scopes = arrayOf(Scope.KEYS_VIEW)
      }
    }
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
      addKey {
        name = "state test key 6"
      }.build {
        addTranslation {
          language = germanLanguage
          text = null
          state = TranslationState.DISABLED
        }
      }
    }
  }

  fun addTwoNamespacesTranslations() {
    projectBuilder.apply {
      addKey("ns-1", "1").addTranslation("en", "1")
      addKey("ns-2", "1").addTranslation("en", "2")
    }
  }

  fun addKeyWithDot() {
    projectBuilder.addKey {
      name = "key.with.dots"
    }
  }

  fun addSentenceKey(): KeyBuilder {
    return projectBuilder.addKey {
      name = "How strong of a variation to produce. At 0, " +
        "there will be no effect. At 1, you will get the " +
        "complete picture with variation seed (except for ancestral " +
        "samplers, where you will just get something)."
    }
  }

  fun addKeysWithScreenshots() {
    var screenshot1: Screenshot? = null

    projectBuilder
      .addKey {
        name = "key with screenshot"
      }.build {
        screenshot1 = addScreenshot {}.self
        addScreenshot {}
      }
    projectBuilder
      .addKey {
        name = "key with screenshot 2"
      }.build {
        addScreenshot {}
        projectBuilder.addScreenshotReference {
          screenshot = screenshot1!!
          key = this@build.self
          originalText = "Oh yeah"
          positions = mutableListOf(KeyInScreenshotPosition(100, 100, 50, 50))
        }
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
        addTag("Cool tag")
        addTranslation {
          language = germanLanguage
          text = "Key with tag DE"
        }
        addTranslation {
          language = englishLanguage
          text = "Key with tag EN"
        }
      }
      addKey {
        name = "Another key with tag"
      }.build {
        addTag("Another cool tag")
        addTranslation {
          language = germanLanguage
          text = "Another key with tag DE"
        }
        addTranslation {
          language = englishLanguage
          text = "Another key with tag EN"
        }
      }
      addKey {
        name = "Key with tag 2"
      }.build {
        addTag("Cool tag")
        addTranslation {
          language = germanLanguage
          text = "Key with tag 2 DE"
        }
        addTranslation {
          language = englishLanguage
          text = "Key with tag 2 EN"
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

  fun generateCursorWithDupeTestData() {
    root.data.projects[0].apply {
      addKey { name = "a" }.build {
        addTranslation {
          language = germanLanguage
          text = "a"
        }
      }
      (1..5).forEach {
        addKey { name = "key-$it" }.build {
          addTranslation {
            language = germanLanguage
            text = "Key text..."
          }
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
              author = user
              text = "Comment $it"
            }
          }
        }
        addTranslation {
          language = englishLanguage
          text = "d"
        }.build {
          (1..3).forEach {
            addComment {
              author = user
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
      addKey {
        name = "hello+i+am+plus+scoped"
      }.build {
        addTranslation {
          text = "yupee!"
          language = englishLanguage
        }
      }
    }
  }

  fun addPlural() {
    projectBuilder.run {
      addKey {
        name = "i am plural"
        isPlural = true
        pluralArgName = "count"
      }.build {
        addTranslation {
          text = "{count, plural, one {I am one} other {I am other}}"
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
            author = user
            text = "aaaa"
            state = TranslationCommentState.RESOLVED
          }
          addComment {
            author = user
            text = "aaaa"
            state = TranslationCommentState.RESOLVED
          }
          addComment {
            author = user
            text = "aaaa"
            state = TranslationCommentState.NEEDS_RESOLUTION
          }
          addComment {
            author = user
            text = "aaaa"
            state = TranslationCommentState.NEEDS_RESOLUTION
          }
        }
      }
    }
  }

  fun addFailedBatchJob(): BatchJob {
    return projectBuilder
      .addBatchJob {
        status = BatchJobStatus.FAILED
      }.build {
        this.targetProvider = {
          listOf(mapOf("keyId" to aKey.id, "languageId" to germanLanguage.id))
        }
        addChunkExecution {
          this.retry = false
          status = BatchJobChunkExecutionStatus.FAILED
        }.build {
          this.successfulTargetsProvider = { emptyList() }
        }
      }.self
  }

  fun addPluralKey(): Key {
    return projectBuilder
      .addKey {
        name = "plural_key"
        isPlural = true
      }.self
  }
}
