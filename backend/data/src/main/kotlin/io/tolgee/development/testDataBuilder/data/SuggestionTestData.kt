package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.mtServiceConfig.Formality
import net.datafaker.Faker
import java.util.*

class SuggestionTestData : BaseTestData() {
  var germanLanguage: Language
  lateinit var beautifulKey: Key
  lateinit var beautifulKeyBuilder: KeyBuilder
  lateinit var thisIsBeautifulKey: Key
  val fakerEn: Faker = Faker(Locale.ENGLISH)
  val fakerDe: Faker = Faker(Locale.GERMAN)

  init {
    projectBuilder.apply {
      germanLanguage =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }.self
      addKeys()
      addKeyDistances()
    }
  }

  private fun ProjectBuilder.addKeys() {
    addKey {
      name = "key 1"
    }.build keyBuilder@{
      addTranslation {
        language = englishLanguage
        key = this@keyBuilder.self
        text = "Beautiful"
      }
      addTranslation {
        language = germanLanguage
        key = this@keyBuilder.self
        text = "Wunderschönen"
      }
    }
    addKey {
      name = "key 2"
    }.build keyBuilder@{
      addTranslation {
        language = englishLanguage
        key = this@keyBuilder.self
        text = "This is beautiful"
      }
      addTranslation {
        language = germanLanguage
        key = this@keyBuilder.self
        text = "Das ist schön"
      }
    }
    addKey {
      name = "key 5"
      thisIsBeautifulKey = this
    }.build keyBuilder@{
      addTranslation {
        language = englishLanguage
        key = this@keyBuilder.self
        text = "This is beautiful even more"
      }
      addTranslation {
        language = germanLanguage
        key = this@keyBuilder.self
        text = "Das ist sehr schön"
      }
    }
    addKey {
      name = "key 3"
    }.build keyBuilder@{
      addTranslation {
        language = englishLanguage
        key = this@keyBuilder.self
        text = "This is different"
      }
      addTranslation {
        language = germanLanguage
        key = this@keyBuilder.self
        text = "Das ist anders"
      }
    }
    addKey {
      name = "key 4"
      beautifulKey = this
    }.build keyBuilder@{
      beautifulKeyBuilder = this
      addTranslation {
        language = englishLanguage
        key = this@keyBuilder.self
        text = "Beautiful"
      }
    }
  }

  private fun ProjectBuilder.addKeyDistances() {
    this.addKeysDistance(data.keys[0].self, data.keys[1].self) {
      distance = 1000.0
    }
    this.addKeysDistance(data.keys[0].self, data.keys[2].self) {
      distance = 8.0
    }
    this.addKeysDistance(data.keys[0].self, data.keys[3].self) {
      distance = 8.0
    }
    this.addKeysDistance(data.keys[0].self, data.keys[4].self) {
      distance = 8.0
    }
    this.addKeysDistance(data.keys[1].self, data.keys[2].self) {
      distance = 2.0
    }
    this.addKeysDistance(data.keys[1].self, data.keys[3].self) {
      distance = 1.0
    }
    this.addKeysDistance(data.keys[1].self, data.keys[4].self) {
      distance = 1.0
    }
    this.addKeysDistance(data.keys[2].self, data.keys[3].self) {
      distance = 1.0
    }
    this.addKeysDistance(data.keys[2].self, data.keys[4].self) {
      distance = 1.0
    }
    this.addKeysDistance(data.keys[3].self, data.keys[4].self) {
      distance = 1.0
    }
  }

  fun enableAWS(formality: Formality = Formality.DEFAULT) {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.AWS)
      this.primaryService = MtServiceType.AWS
      this.awsFormality = formality
    }
  }

  fun enableDeepL(formality: Formality = Formality.DEFAULT) {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.DEEPL)
      this.primaryService = MtServiceType.DEEPL
      this.deeplFormality = formality
    }
  }

  fun enablePrompt(formality: Formality = Formality.DEFAULT) {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.PROMPT)
      this.primaryService = MtServiceType.PROMPT
      this.promptFormality = formality
    }
  }

  fun enableAll() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices =
        mutableSetOf(
          MtServiceType.GOOGLE,
          MtServiceType.AWS,
          MtServiceType.DEEPL,
          MtServiceType.AZURE,
          MtServiceType.BAIDU,
          MtServiceType.PROMPT,
        )
      this.primaryService = MtServiceType.AWS
    }
  }

  fun enableAllGooglePrimary() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices =
        mutableSetOf(
          MtServiceType.GOOGLE,
          MtServiceType.AWS,
          MtServiceType.DEEPL,
          MtServiceType.AZURE,
          MtServiceType.BAIDU,
        )
      this.primaryService = MtServiceType.GOOGLE
    }
  }

  fun addDefaultConfig() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = null
      this.enabledServices = mutableSetOf(MtServiceType.AWS)
      this.primaryService = MtServiceType.AWS
    }
  }

  fun generateLotOfData() {
    projectBuilder.apply {
      (0..1000).forEach {
        addKey {
          name = UUID.randomUUID().toString()
        }.build keyBuilder@{
          addTranslation {
            key = this@keyBuilder.self
            language = englishLanguage
            text = "Beautiful " + fakerEn.funnyName().name() + " " + fakerEn.funnyName().name()
          }

          addTranslation {
            key = this@keyBuilder.self
            language = germanLanguage
            text = "Wunderschönen " + fakerDe.funnyName().name() + " " + fakerDe.funnyName().name()
          }
        }
      }
    }
  }

  fun addBucketWithExtraCredits() {
    userAccountBuilder.defaultOrganizationBuilder.addMtCreditBucket {
      credits = 1000
      extraCredits = 1000
    }
  }

  fun addAiDescriptions() {
    beautifulKeyBuilder.addMeta { description = "This key is Beautiful" }
    germanLanguage.aiTranslatorPromptDescription = "This is a description for AI translator"
    project.aiTranslatorPromptDescription = "This is a description for AI translator"
  }

  fun addPluralKeys(): PluralKeys {
    return PluralKeys(
      addPluralKey("true plural", true),
      addPluralKey("same true plural", true),
      addPluralKey("false plural", false),
      addPluralKey("same false plural", false),
    )
  }

  data class PluralKeys(
    val truePlural: Key,
    val sameTruePlural: Key,
    val falsePlural: Key,
    val sameFalsePlural: Key,
  )

  private fun addPluralKey(
    name: String,
    isPlural: Boolean,
  ): Key {
    val isNotPluralString = if (isPlural) "" else "not plural"
    return projectBuilder.addKey(name) {
      this.self.isPlural = isPlural
      addTranslation {
        language = englishLanguage
        text = "{value, plural, one {# dog} other {# dogs}}$isNotPluralString"
      }
      addTranslation {
        language = germanLanguage
        text = "{value, plural, one {# Hund} other {# Hunde}}$isNotPluralString"
      }
    }.self
  }
}
