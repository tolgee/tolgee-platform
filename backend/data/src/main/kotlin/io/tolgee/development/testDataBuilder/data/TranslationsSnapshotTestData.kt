package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState

/**
 * Self-contained test data for [TranslationsControllerSnapshotTest].
 *
 * Creates a project with English (base) and German, then populates:
 * - "A key" — German REVIEWED/outdated/auto with resolved comment, tagged "Cool tag"
 * - "Z key" — English auto-translated, tagged "Lame tag" + "Some other tag"
 * - "commented_key" — German with 2 resolved + 2 unresolved comments
 * - 3 tagged keys ("Key with tag", "Another key with tag", "Key with tag 2")
 * - 6 state-test keys covering REVIEWED, TRANSLATED, DISABLED, auto, and missing translations
 * - 1 branched key (excluded by default branch filter)
 */
class TranslationsSnapshotTestData : BaseTestData("franta", "Franta's project") {
  lateinit var germanLanguage: Language

  init {
    projectBuilder.self.useBranching = true

    germanLanguage =
      projectBuilder
        .addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }.self

    projectBuilder.apply {
      // ===== "A key" — base key with German REVIEWED, outdated, auto, mtProvider, comment, tag =====
      addKey {
        name = "A key"
      }.build {
        setDescription("A key description")
        addTranslation {
          language = germanLanguage
          text = "Z translation"
          state = TranslationState.REVIEWED
          auto = true
          outdated = true
          mtProvider = MtServiceType.GOOGLE
          self.translations.add(this)
        }.build {
          addComment {
            author = this@TranslationsSnapshotTestData.user
            text = "Comment"
            state = TranslationCommentState.RESOLVED
          }
        }
        addTag("Cool tag")
      }

      // ===== "Z key" — English auto-translated with tags =====
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

      // ===== Branched key (excluded by default branch filter) =====
      addBranch {
        name = "test-branch"
        project = this@apply.self
      }.build {
        addKey {
          name = "branch key"
          branch = this@build.self
        }.build {
          addTranslation {
            language = germanLanguage
            text = "Branched german key."
          }
          addTranslation {
            language = englishLanguage
            text = "Branched english key."
          }
        }
      }

      // ===== "commented_key" — 2 resolved + 2 unresolved comments on German =====
      addKey {
        name = "commented_key"
      }.build {
        addTranslation {
          language = germanLanguage
          text = "Nice"
        }.build {
          addComment {
            author = this@TranslationsSnapshotTestData.user
            text = "aaaa"
            state = TranslationCommentState.RESOLVED
          }
          addComment {
            author = this@TranslationsSnapshotTestData.user
            text = "aaaa"
            state = TranslationCommentState.RESOLVED
          }
          addComment {
            author = this@TranslationsSnapshotTestData.user
            text = "aaaa"
            state = TranslationCommentState.NEEDS_RESOLUTION
          }
          addComment {
            author = this@TranslationsSnapshotTestData.user
            text = "aaaa"
            state = TranslationCommentState.NEEDS_RESOLUTION
          }
        }
      }

      // ===== Tagged keys =====
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

      // ===== State-test keys =====
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
}
