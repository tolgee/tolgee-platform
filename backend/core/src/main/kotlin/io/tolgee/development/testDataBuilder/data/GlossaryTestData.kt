package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation

class GlossaryTestData {
  lateinit var userOwner: UserAccount
  lateinit var userMaintainer: UserAccount
  lateinit var userMember: UserAccount
  lateinit var organization: Organization
  lateinit var project: Project
  lateinit var glossary: Glossary
  lateinit var term: GlossaryTerm
  lateinit var translation: GlossaryTermTranslation

  lateinit var anotherProject: Project
  lateinit var anotherProject2: Project

  lateinit var trademarkTerm: GlossaryTerm
  lateinit var forbiddenTerm: GlossaryTerm

  lateinit var emptyGlossary: Glossary

  lateinit var userUnaffiliated: UserAccount

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      addUserAccount {
        username = "Unaffiliated"
      }.build {
        userUnaffiliated = self

        addProject(defaultOrganizationBuilder.self) {
          name = "TheEmptyProject"
        }.self
      }

      addUserAccount {
        username = "Owner"
      }.build {
        userOwner = self

        project =
          addProject(defaultOrganizationBuilder.self) {
            name = "TheProject"
          }.build {
            val english = addEnglish()
            val french = addFrench()
            val czech = addCzech()

            self.baseLanguage = english.self

            addKey {
              name = "key_with_term"
            }.build {
              addTranslation {
                language = english.self
                text = "This is a Term that should be highlighted"
              }
              addTranslation {
                language = french.self
                text = "C'est un terme qui devrait être mis en évidence"
              }
              addTranslation {
                language = czech.self
                text = "Toto je termín, který by měl být zvýrazněn"
              }
            }

            addKey {
              name = "key_without_term"
            }.build {
              addTranslation {
                language = english.self
                text = "This is a text"
              }
              addTranslation {
                language = french.self
                text = "C'est un texte"
              }
              addTranslation {
                language = czech.self
                text = "Toto je text"
              }
            }
          }.self

        anotherProject =
          addProject(defaultOrganizationBuilder.self) {
            name = "Another1"
          }.build {
            self.baseLanguage = addCzech().self
            addEnglish()
            addGerman()
          }.self

        anotherProject2 =
          addProject(defaultOrganizationBuilder.self) {
            name = "Another2"
          }.build {
            self.baseLanguage = addEnglish().self
            addCzech()
            addGerman()
          }.self

        defaultOrganizationBuilder.build {
          organization = self

          addRole {
            user =
              addUserAccount {
                username = "Maintainer"
              }.build {
                userMaintainer = self
              }.self
            type = OrganizationRoleType.MAINTAINER
          }

          addRole {
            user =
              addUserAccount {
                username = "Member"
              }.build {
                userMember = self
              }.self
            type = OrganizationRoleType.MEMBER
          }

          glossary =
            addGlossary {
              name = "Test Glossary"
              baseLanguageTag = "en"
            }.build {
              assignProject(project)
              term =
                addTerm {
                  description = "The description"
                }.build {
                  translation =
                    addTranslation {
                      languageTag = "en"
                      text = "Term"
                    }.self
                }.self

              trademarkTerm =
                addTerm {
                  description = "Trademark"
                  flagNonTranslatable = true
                  flagCaseSensitive = true
                }.build {
                  addTranslation {
                    languageTag = "en"
                    text = "Apple"
                  }
                }.self

              forbiddenTerm =
                addTerm {
                  description = "Forbidden term"
                  flagForbiddenTerm = true
                }.build {
                  addTranslation {
                    languageTag = "en"
                    text = "fun"
                  }

                  addTranslation {
                    languageTag = "cs"
                    text = "zábava"
                  }
                }.self

              addTerm {
                description = "The multiword term"
                flagAbbreviation = true
              }.build {
                addTranslation {
                  languageTag = "en"
                  text = "A.B.C Inc"
                }
                addTranslation {
                  languageTag = "cs"
                  text = "A.B.C, s.r.o."
                }
              }
            }.self

          emptyGlossary =
            addGlossary {
              name = "Empty Glossary"
              baseLanguageTag = "cs"
            }.build {
              assignProject(project)
            }.self
        }
      }
    }
}
