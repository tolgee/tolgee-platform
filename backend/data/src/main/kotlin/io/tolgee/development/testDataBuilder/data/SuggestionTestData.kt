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
        self {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }
      }.self

      addKey {
        self {
          name = "key 1"
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "Beautiful"
          }
        }
        addTranslation {
          self {
            language = germanLanguage
            key = this@addKey.self
            text = "Wunderschönen"
          }
        }
      }
      addKey {
        self {
          name = "key 2"
          thisIsBeautifulKey = this
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "This is beautiful"
          }
        }
        addTranslation {
          self {
            language = germanLanguage
            key = this@addKey.self
            text = "Das ist schön"
          }
        }
      }
      addKey {
        self {
          name = "key 5"
          thisIsBeautifulKey = this
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "This is beautiful even more"
          }
        }
        addTranslation {
          self {
            language = germanLanguage
            key = this@addKey.self
            text = "Das ist sehr schön"
          }
        }
      }
      addKey {
        self {
          name = "key 3"
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "This is different"
          }
        }
        addTranslation {
          self {
            language = germanLanguage
            key = this@addKey.self
            text = "Das ist anders"
          }
        }
      }
      addKey {
        self {
          name = "key 4"
          beautifulKey = this
        }
        addTranslation {
          self {
            language = englishLanguage
            key = this@addKey.self
            text = "Beautiful"
          }
        }
      }
    }
  }

  fun enableAWS() {
    projectBuilder.addMtServiceConfig {
      self {
        this.targetLanguage = germanLanguage
        this.enabledServices = mutableSetOf(MtServiceType.AWS)
        this.primaryService = MtServiceType.AWS
      }
    }
  }

  fun enableBoth() {
    projectBuilder.addMtServiceConfig {
      self {
        this.targetLanguage = germanLanguage
        this.enabledServices = mutableSetOf(MtServiceType.GOOGLE, MtServiceType.AWS)
        this.primaryService = MtServiceType.AWS
      }
    }
  }

  fun addDefaultConfig() {
    projectBuilder.addMtServiceConfig {
      self {
        this.targetLanguage = null
        this.enabledServices = mutableSetOf(MtServiceType.AWS)
        this.primaryService = MtServiceType.AWS
      }
    }
  }

  fun generateLotOfData() {
    projectBuilder.apply {
      (0..10000).forEach {
        addKey {
          self {
            name = UUID.randomUUID().toString()
          }

          addTranslation {
            self {
              key = this@addKey.self
              language = englishLanguage
              text = "Beautiful " + fakerEn.funnyName().name() + " " + fakerEn.funnyName().name()
            }
          }

          addTranslation {
            self {
              key = this@addKey.self
              language = germanLanguage
              text = "Wunderschönen " + fakerDe.funnyName().name() + " " + fakerDe.funnyName().name()
            }
          }
        }
      }
    }
  }
}
