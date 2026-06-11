package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm

/**
 * Two organizations, each owning a glossary, used to exercise app org-level glossary access:
 * an app installed in [organization] (org A) may touch [glossary] but never [otherGlossary]
 * (org B). [user] is only an org MEMBER (not a maintainer), so app-token writes that succeed
 * prove the install grant — not the user's role — is what authorizes.
 */
class GlossaryAppAccessTestData {
  lateinit var user: UserAccount
  lateinit var userMaintainer: UserAccount
  lateinit var organization: Organization
  lateinit var project: Project
  lateinit var glossary: Glossary
  lateinit var term: GlossaryTerm
  lateinit var otherOrganization: Organization
  lateinit var otherGlossary: Glossary

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val appUser = addUserAccount { username = "app-user" }.self
      user = appUser
      val maintainer = addUserAccount { username = "glossary-maintainer" }.self
      userMaintainer = maintainer

      val orgABuilder = addOrganization { name = "App Org" }
      organization = orgABuilder.self
      project = addProject(organization) { name = "App Project" }.self

      orgABuilder.build {
        addRole {
          this.user = appUser
          type = OrganizationRoleType.MEMBER
        }
        addRole {
          this.user = maintainer
          type = OrganizationRoleType.MAINTAINER
        }
        glossary =
          addGlossary {
            name = "Test Glossary"
            baseLanguageTag = "en"
          }.build {
            assignProject(project)
            term =
              addTerm { description = "A term" }
                .build {
                  addTranslation {
                    languageTag = "en"
                    text = "Term"
                  }
                }.self
          }.self
      }

      val orgBBuilder = addOrganization { name = "Other Org" }
      otherOrganization = orgBBuilder.self
      orgBBuilder.build {
        otherGlossary =
          addGlossary {
            name = "Other Glossary"
            baseLanguageTag = "en"
          }.self
      }
    }
}
