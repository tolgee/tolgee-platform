package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm

class GlossaryAiTranslationTestData {
  lateinit var userOwner: UserAccount
  lateinit var project: Project
  lateinit var glossary: Glossary

  lateinit var translatedTerm: GlossaryTerm
  lateinit var untranslatedTerm: GlossaryTerm
  lateinit var untranslatedWithDescriptionTerm: GlossaryTerm
  lateinit var nonTranslatableTerm: GlossaryTerm
  lateinit var forbiddenTerm: GlossaryTerm
  lateinit var caseSensitiveOnlyTerm: GlossaryTerm
  lateinit var abbreviationOnlyTerm: GlossaryTerm

  val sourceText = "Apple Banana Cherry Dragon Elder Fig Grape are common words"

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      addUserAccount {
        username = "Owner"
      }.build {
        userOwner = self

        project =
          addProject(defaultOrganizationBuilder.self) {
            name = "GlossaryAiProject"
          }.build {
            val english = addEnglish()
            addFrench()
            self.baseLanguage = english.self
          }.self

        defaultOrganizationBuilder.build {
          glossary =
            addGlossary {
              name = "AI Glossary"
              baseLanguageTag = "en"
            }.build {
              assignProject(project)

              translatedTerm =
                addTerm {}
                  .build {
                    addTranslation {
                      languageTag = "en"
                      text = "Apple"
                    }
                    addTranslation {
                      languageTag = "fr"
                      text = "Pomme"
                    }
                  }.self

              untranslatedTerm =
                addTerm {}
                  .build {
                    addTranslation {
                      languageTag = "en"
                      text = "Banana"
                    }
                  }.self

              untranslatedWithDescriptionTerm =
                addTerm {
                  description = "Translate as the fruit, never as a name"
                }.build {
                  addTranslation {
                    languageTag = "en"
                    text = "Cherry"
                  }
                }.self

              nonTranslatableTerm =
                addTerm {
                  flagNonTranslatable = true
                }.build {
                  addTranslation {
                    languageTag = "en"
                    text = "Dragon"
                  }
                }.self

              forbiddenTerm =
                addTerm {
                  flagForbiddenTerm = true
                }.build {
                  addTranslation {
                    languageTag = "en"
                    text = "Elder"
                  }
                }.self

              caseSensitiveOnlyTerm =
                addTerm {
                  flagCaseSensitive = true
                }.build {
                  addTranslation {
                    languageTag = "en"
                    text = "Fig"
                  }
                }.self

              abbreviationOnlyTerm =
                addTerm {
                  flagAbbreviation = true
                }.build {
                  addTranslation {
                    languageTag = "en"
                    text = "Grape"
                  }
                }.self
            }.self
        }
      }
    }
}
