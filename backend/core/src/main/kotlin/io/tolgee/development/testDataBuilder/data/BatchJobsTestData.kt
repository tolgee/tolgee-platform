package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Label

class BatchJobsTestData : BaseTestData() {
  val anotherUser = root.addUserAccount { username = "anotherUser" }.self
  val germanLanguage = projectBuilder.addGerman().self
  val czechLanguage = projectBuilder.addCzech().self

  init {
    this.projectBuilder.addPermission {
      user = anotherUser
      scopes = arrayOf(Scope.KEYS_VIEW)
    }
  }

  fun addTranslationOperationData(keyCount: Int = 100): List<Key> {
    addAKey()
    addLabels(2)
    return (1..keyCount).map {
      this.projectBuilder
        .addKey {
          name = "key$it"
        }.build {
          addTranslation {
            language = englishLanguage
            text = "en"
          }
        }.self
    }
  }

  private fun addAKey(): KeyBuilder {
    return this.projectBuilder
      .addKey {
        name = "a-key"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "en"
        }
        addTranslation {
          language = czechLanguage
          text = "cs"
        }
        addTranslation {
          language = germanLanguage
          text = "de"
        }
      }
  }

  fun addKeyWithTranslationsReviewed(): Key {
    val aKey = addAKey()
    aKey.translations.forEach {
      it.self.state = TranslationState.REVIEWED
    }
    return aKey.self
  }

  fun addStateChangeData(keyCount: Int = 100): List<Key> {
    return (1..keyCount).map {
      this.projectBuilder
        .addKey {
          name = "key$it"
        }.build {
          addTranslation {
            language = englishLanguage
            text = "en$it"
          }
          addTranslation {
            language = germanLanguage
            text = "de$it"
          }
          addTranslation {
            language = czechLanguage
            text = "cs$it"
          }
        }.self
    }
  }

  fun addTagKeysData(keyCount: Int = 100): List<Key> {
    this.projectBuilder
      .addKey {
        name = "a-key"
      }.build {
        addTag("a-tag")
      }.self
    return (1..keyCount).map {
      this.projectBuilder
        .addKey {
          name = "key$it"
        }.build {
          addTag("tag1")
          addTag("tag2")
          addTag("tag3")
        }.self
    }
  }

  fun addNamespaceData(): List<Key> {
    this.projectBuilder.addKey("namespace", "key")
    return (1..500).map {
      this.projectBuilder.addKey("namespace1", "key$it").self
    } +
      (1..500).map {
        this.projectBuilder.addKey(keyName = "key-without-namespace-$it").self
      }
  }

  fun addLabels(count: Int): List<Label> {
    return (1..count).map {
      projectBuilder
        .addLabel {
          name = "Label $it"
          color = ("#${"%06x".format((0..0xFFFFFF).random())}")
          project = projectBuilder.self
        }.self
    }
  }

  fun addKeysWithLanguages(
    count: Int,
    labels: List<Label>? = listOf(),
  ): List<Key> {
    val languages = projectBuilder.data.languages.map { it.self }

    return (1..count).map {
      this.projectBuilder
        .addKey {
          name = "key$it"
        }.build {
          languages.forEach { language ->
            addTranslation {
              this.language = language
              text = "text$it"
              labels?.forEach { label ->
                addLabel(label)
              }
            }
          }
        }.self
    }
  }

  fun addEmptyKeys(count: Int): List<Key> {
    return (1..count).map {
      this.projectBuilder
        .addKey {
          name = "key$it"
        }.self
    }
  }
}
