package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation

class LabelsTestData : BaseTestData("labelsTestUser", "labelsTestProject") {
  private lateinit var firstTranslation: Translation
  var firstLabel: Label

  init {
    projectBuilder.apply {
      addKey {
        name = "first key"
      }.build {
        firstTranslation = addTranslation("en", "first key translation").self
      }
      firstLabel = addLabel {
        name = "First label"
        color = "#FF0000"
        description = "This is a description"
        translations = mutableSetOf(firstTranslation)
        project = projectBuilder.self
      }.self
    }
  }
}
