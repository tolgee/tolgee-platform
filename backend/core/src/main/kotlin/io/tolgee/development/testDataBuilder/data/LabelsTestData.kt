package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation

class LabelsTestData : BaseTestData() {
  lateinit var labeledTranslation: Translation
  lateinit var firstLabel: Label
  lateinit var secondLabel: Label
  lateinit var unassignedTranslation: Translation
  lateinit var unassignedLabel: Label
  lateinit var czechLanguage: Language
  lateinit var keyWithoutCzTranslation: Key

  init {
    root.apply {
      projectBuilder.apply {
        addCzech().build {
          czechLanguage = this.self
        }
        addKey {
          name = "first key"
        }.build {
          labeledTranslation = addTranslation("en", "first key translation").self
        }.build {
          unassignedTranslation = addTranslation("cs", "first key second translation").self
        }.build {
          firstLabel =
            addLabel {
              name = "First label"
              color = "#FF0000"
              description = "This is a description"
              translations.add(labeledTranslation)
              project = projectBuilder.self
            }.self
          unassignedLabel =
            addLabel {
              name = "Unassigned label"
              color = "#00FF00"
              description = "This is a description for unassigned label"
              project = projectBuilder.self
            }.self
          labeledTranslation.labels.add(firstLabel)
          for (i in 1..3) {
            addLabel {
              name = "label to assign $i"
              color = "#FF00FF"
              description = "This is a freshly new label $i"
              project = projectBuilder.self
            }
          }
        }
        addKey {
          name = "second key"
        }.build {
          addTranslation("en", "second key translation").self
        }.build {
          addTranslation("cs", "second key second translation").self
        }
        addKey {
          name = "third key (without czech translations)"
        }.build {
          addTranslation("en", "third key translation").self
          keyWithoutCzTranslation = this.self
        }
      }
      addProject {
        name = "Second project"
      }.build secondProject@{
        val key =
          addKey {
            name = "Second project key"
          }
        val en = addEnglish()
        val translation =
          addTranslation {
            language = en.self
            text = "second project key translation"
            this.key = key.self
          }.self
        secondLabel =
          addLabel {
            name = "Second project label"
            color = "#FF0000"
            description = "This is a description"
            translations = mutableSetOf(translation)
            project = this@secondProject.self
          }.self
        for (i in 1..25) {
          addLabel {
            name = "Label $i"
            color = listOf("#8995A5", "#FF921E", "#35C4B0", "#9154FB", "#1188FF", "#FF2E2E").random()
            description = "This is a description for label $i"
            translations = mutableSetOf(translation)
            translation.labels.add(this)
            project = this@secondProject.self
          }
        }
      }
      addProject { name = "Project without labels" }
    }
  }
}
