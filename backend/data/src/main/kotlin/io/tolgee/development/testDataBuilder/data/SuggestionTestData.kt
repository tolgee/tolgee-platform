package io.tolgee.development.testDataBuilder.data

import com.github.javafaker.Faker
import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.key.Key
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
        thisIsBeautifulKey = this
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
  }

  fun enableAWS() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.AWS)
      this.primaryService = MtServiceType.AWS
    }
  }

  fun enableBoth() {
    projectBuilder.addMtServiceConfig {

      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.GOOGLE, MtServiceType.AWS)
      this.primaryService = MtServiceType.AWS
    }
  }

  fun enableBothGooglePrimary() {
    projectBuilder.addMtServiceConfig {
      this.targetLanguage = germanLanguage
      this.enabledServices = mutableSetOf(MtServiceType.GOOGLE, MtServiceType.AWS)
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
}
