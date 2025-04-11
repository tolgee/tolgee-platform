package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.builders.*
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType

class PromptTestData : BaseTestData() {
  val organization: OrganizationBuilder
  val unrelatedOrganization: OrganizationBuilder
  val organizationMember: UserAccountBuilder
  val organizationOwner: UserAccountBuilder
  val promptProject: ProjectBuilder
  val projectEditor: UserAccountBuilder
  val projectReviewer: UserAccountBuilder
  lateinit var english: LanguageBuilder
  lateinit var czech: LanguageBuilder
  lateinit var keys: MutableList<KeyBuilder>
  lateinit var customPrompt: PromptBuilder
  lateinit var llmProvider: LLMProviderBuilder

  init {
    organizationMember =
      root.addUserAccount {
        username = "member@organization.com"
      }
    organizationOwner =
      root.addUserAccount {
        username = "owner@organization.com"
      }

    unrelatedOrganization =
      root.addOrganization {
        name = "unrelated organization"
      }

    organization =
      root.addOrganization {
        name = "my organization"
      }.build {
        addRole {
          user = organizationMember.self
          organization = this.organization
          type = OrganizationRoleType.MEMBER
        }

        addRole {
          user = organizationOwner.self
          organization = this.organization
          type = OrganizationRoleType.OWNER
        }

        llmProvider =
          addLLMProvider {
            name = "organization-provider"
          }
      }

    projectEditor =
      root.addUserAccount {
        username = "projectEditor@organization.com"
      }
    projectReviewer =
      root.addUserAccount {
        username = "projectReviewer@organization.com"
      }

    promptProject =
      root.addProject {
        organizationOwner = organization.self
        name = "Prompt project"
      }.build {
        english = addEnglish()
        czech = addCzech()

        keys =
          listOf(1, 2, 3, 4).map { i ->
            addKey("Key $i").also {
              addTranslation {
                key = it.self
                text = "English translation $i"
                language = english.self
              }
              addTranslation {
                key = it.self
                text = "Czech translation $i"
                language = czech.self
              }
            }
          }.toMutableList()

        addPermission {
          user = projectEditor.self
          type = ProjectPermissionType.EDIT
        }

        addPermission {
          user = projectReviewer.self
          type = ProjectPermissionType.REVIEW
        }

        customPrompt =
          addPrompt {
            name = "Custom prompt"
            providerName = "organization-provider"
            template =
              """
              Test prompt
              {{fragment.intro}}
              """.trimIndent()
          }
      }
  }
}
