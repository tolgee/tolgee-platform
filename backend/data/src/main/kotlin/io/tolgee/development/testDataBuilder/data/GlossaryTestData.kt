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
  lateinit var organization: Organization
  lateinit var project: Project
  lateinit var glossary: Glossary
  lateinit var term: GlossaryTerm
  lateinit var translation: GlossaryTermTranslation

  lateinit var trademarkTerm: GlossaryTerm
  lateinit var forbiddenTerm: GlossaryTerm

  val root: TestDataBuilder = TestDataBuilder().apply {
    addUserAccount {
      username = "Owner"
    }.build {
      userOwner = self

      project = addProject(defaultOrganizationBuilder.self) {
        name = "TheProject"
      }.self

      defaultOrganizationBuilder.build {
        organization = self

        addRole {
          user = addUserAccount {
            username = "Maintainer"
          }.build {
            userMaintainer = self
          }.self
          type = OrganizationRoleType.MAINTAINER
        }

        glossary = addGlossary {
          name = "Test Glossary"
          baseLanguageTag = "en"
        }.build {
          term = addTerm {
            description = "The description"
          }.build {
            translation = addTranslation {
              languageTag = "en"
              text = "Term"
            }.self
          }.self

          trademarkTerm = addTerm {
            description = "Trademark"
            flagNonTranslatable = true
            flagCaseSensitive = true
          }.build {
            addTranslation {
              languageTag = "en"
              text = "Apple"
            }
          }.self

          forbiddenTerm = addTerm {
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
        }.self

        addGlossary {
          name = "Empty Glossary"
          baseLanguageTag = "cs"
        }
      }
    }
  }
}
