package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation

class GlossaryPermissionsTestData {
  lateinit var userOwner: UserAccount
  lateinit var userMaintainer: UserAccount
  lateinit var userMember: UserAccount
  lateinit var userUnaffiliated: UserAccount
  lateinit var userProjectTranslator: UserAccount

  lateinit var organization: Organization
  lateinit var project: Project
  lateinit var glossary: Glossary
  lateinit var term: GlossaryTerm
  lateinit var translation: GlossaryTermTranslation

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      addUserAccount {
        username = "Owner"
      }.build {
        userOwner = self

        userProjectTranslator =
          addUserAccount {
            username = "ProjectTranslator"
          }.self

        project =
          addProject(defaultOrganizationBuilder.self) {
            name = "TheProject"
          }.build {
            addPermission {
              user = userProjectTranslator
              type = ProjectPermissionType.VIEW
            }
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

          userUnaffiliated =
            addUserAccount {
              username = "Unaffiliated"
            }.self

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
            }.self
        }
      }
    }
}
