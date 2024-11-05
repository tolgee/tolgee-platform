package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.enums.TranslationState

class KeyLanguageDisablingTestData {
  val root: TestDataBuilder = TestDataBuilder()

  val user =
    root.addUserAccount {
      username = "user"
    }.self

  val project = root.addProject { name = "project" }

  val english = project.addEnglish()
  val german = project.addGerman()
  val french = project.addFrench()
  val czech = project.addCzech()

  val key = project.addKey("key")

  val enTranslation =
    key.addTranslation {
      language = english.self
    }

  val deTranslation =
    key.addTranslation {
      language = german.self
      text = null
      state = TranslationState.DISABLED
    }

  val csTranslation =
    key.addTranslation {
      language = czech.self
      text = null
      state = TranslationState.DISABLED
    }

  init {
    project.addKey("notDisabledKey").addTranslation {
      language = english.self
      text = "text"
    }
  }
}
