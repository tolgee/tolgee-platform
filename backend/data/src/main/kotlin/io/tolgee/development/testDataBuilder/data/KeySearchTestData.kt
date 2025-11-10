package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import net.datafaker.Faker
import java.util.UUID

class KeySearchTestData : BaseTestData() {
  init {
    projectBuilder.addGerman()

    projectBuilder.apply {
      addKeyWithTranslations("this-is-key", "Hallo!", "Hello!")
      addKeyWithTranslations("this-is-key-2", "Aj!", "Oh!")
      addKeyWithTranslations("beauty", "Krása!", "Oh!")
    }

    root.apply {
      addProject { name = "another" }.build {
        addEnglish()
        addGerman()
        addKeyWithTranslations("lalala", "Hallo!", "Hello!")
      }
    }
  }

  fun ProjectBuilder.addKeyWithTranslations(
    keyName: String,
    translation: String?,
    baseTranslation: String,
  ) {
    addKey {
      name = keyName
      addTranslation {
        key = this@addKey
        language = getLanguageByTag("en")!!.self
        text = baseTranslation
      }
      addTranslation {
        key = this@addKey
        language = getLanguageByTag("de")!!.self
        text = translation
      }
    }
  }

  fun addRandomKey() {
    this.projectBuilder.addKeyWithTranslations(
      getRandomWords(),
      getRandomWords(),
      getRandomWords(),
    )
  }

  private fun getRandomWords(): String {
    val uuid = UUID.randomUUID().toString()
    return Faker().lorem().words((1..50).random()).joinToString(" ") + " $uuid"
  }
}
