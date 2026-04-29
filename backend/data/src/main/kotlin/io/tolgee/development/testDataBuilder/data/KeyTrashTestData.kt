package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Key

/**
 * Test data for [KeyTrashFilterTest].
 *
 * Creates a project with English (base) and German, 10 numbered keys ("key 01"–"key 10") with
 * translations in both languages, a second user with MANAGE permission, and 3 tagged keys
 * ("Key with tag", "Another key with tag", "Key with tag 2").
 *
 * All keys and users are exposed as public fields so tests can reference them directly without
 * calling service lookups.
 */
class KeyTrashTestData : BaseTestData("franta", "Franta's project") {
  lateinit var secondUser: UserAccount

  val germanLanguage =
    projectBuilder
      .addLanguage {
        name = "German"
        tag = "de"
        originalName = "Deutsch"
      }.self

  /** Numbered keys ("key 01" through "key 10"), indexed 0–9. */
  val numberedKeys = mutableListOf<Key>()

  lateinit var keyWithTag: Key
  lateinit var anotherKeyWithTag: Key
  lateinit var keyWithTag2: Key

  init {
    root.apply {
      secondUser =
        addUserAccount {
          username = "second-deleter"
        }.self
    }

    projectBuilder.apply {
      addPermission {
        this.user = secondUser
        this.type = ProjectPermissionType.MANAGE
      }

      for (i in 1..10) {
        val padNum = i.toString().padStart(2, '0')
        numberedKeys.add(
          addKey {
            name = "key $padNum"
          }.build {
            addTranslation {
              language = germanLanguage
              text = "I am key $padNum's german translation."
            }
            addTranslation {
              language = englishLanguage
              text = "I am key $padNum's english translation."
            }
          }.self,
        )
      }

      keyWithTag =
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
        }.self

      anotherKeyWithTag =
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
        }.self

      keyWithTag2 =
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
        }.self
    }
  }
}
