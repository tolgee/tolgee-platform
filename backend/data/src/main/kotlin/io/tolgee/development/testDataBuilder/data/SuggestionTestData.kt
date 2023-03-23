package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import net.datafaker.Faker
import java.util.*

class SuggestionTestData : BaseTestData() {
  var germanLanguage: Language
  lateinit var beautifulKey: Key
  lateinit var thisIsBeautifulKey: Key
  val fakerEn: Faker = Faker(Locale.ENGLISH)
  val fakerDe: Faker = Faker(Locale.GERMAN)

  init {
    projectBuilder.apply {
      germanLanguage = addLanguage {
        name = "German"
        tag = "de"
        originalName = "Deutsch"
      }.self

      addKeys()
      addBigMetas()
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
      addTranslation {
        language = englishLanguage
        key = this@keyBuilder.self
        text = "Beautiful"
      }
    }
  }

  private fun ProjectBuilder.addBigMetas() {
    addBigMeta {
      keyName = "key 4"
      namespace = null
      location = "home"
      this.contextData = listOf("I am other item")
    }

    addBigMeta {
      keyName = "key 1"
      namespace = null
      location = "home"
      this.contextData = listOf("I am an example data item")
    }
  }

  fun enableAWS() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.AWS)
      this.primaryService = MtServiceType.AWS
    }
  }

  fun enableTolgee() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.TOLGEE)
      this.primaryService = MtServiceType.TOLGEE
    }
  }

  fun enableAll() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices =
        mutableSetOf(
          MtServiceType.GOOGLE, MtServiceType.AWS, MtServiceType.DEEPL, MtServiceType.AZURE,
          MtServiceType.BAIDU, MtServiceType.TOLGEE
        )
      this.primaryService = MtServiceType.AWS
    }
  }

  fun enableAllGooglePrimary() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices =
        mutableSetOf(
          MtServiceType.GOOGLE, MtServiceType.AWS, MtServiceType.DEEPL, MtServiceType.AZURE,
          MtServiceType.BAIDU
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
}
