package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation

class LabelsTestData : BaseTestData() {
  private lateinit var firstTranslation: Translation
  private lateinit var secondTranslation: Translation
  lateinit var firstLabel: Label
  lateinit var secondLabel: Label

  init {
    root.apply {
      projectBuilder.apply {
        addKey {
          name = "first key"
        }.build {
          firstTranslation = addTranslation("en", "first key translation").self
        }.build {
          firstLabel = addLabel {
            name = "First label"
            color = "#FF0000"
            description = "This is a description"
            translations = mutableSetOf(firstTranslation)
            firstTranslation.labels.add(this)
            project = projectBuilder.self
          }.self
        }
      }
      addProject {
        name = "Second project"
      }.build secondProject@{
        val key = addKey {
          name = "Second project key"
        }
        val en = addEnglish()
        secondTranslation = addTranslation {
          language = en.self
          text = "second project key translation"
          this.key = key.self
        }.self
        secondLabel = addLabel {
          name = "Second project label"
          color = "#FF0000"
          description = "This is a description"
          translations = mutableSetOf(secondTranslation)
          project = this@secondProject.self
        }.self
        for (i in 1..25) {
          addLabel {
            name = "Label $i"
            color = "#FF0000"
            description = "This is a description for label $i"
            translations = mutableSetOf(secondTranslation)
            project = this@secondProject.self
          }
        }
      }
    }
  }
}
