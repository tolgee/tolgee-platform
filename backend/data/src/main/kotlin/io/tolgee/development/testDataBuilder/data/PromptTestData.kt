package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.LanguageBuilder
import io.tolgee.development.testDataBuilder.builders.LlmProviderBuilder
import io.tolgee.development.testDataBuilder.builders.OrganizationBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.PromptBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType

class PromptTestData : BaseTestData() {
  val organization: OrganizationBuilder
  val unrelatedOrganization: OrganizationBuilder
  val serverAdmin: UserAccountBuilder
  val organizationMember: UserAccountBuilder
  val organizationOwner: UserAccountBuilder
  val promptProject: ProjectBuilder
  val projectEditor: UserAccountBuilder
  val projectReviewer: UserAccountBuilder
  lateinit var english: LanguageBuilder
  lateinit var czech: LanguageBuilder
  lateinit var keys: MutableList<KeyBuilder>
  lateinit var customPrompt: PromptBuilder
  lateinit var llmProvider: LlmProviderBuilder

  init {

    serverAdmin =
      root.addUserAccount {
        username = "admin@admin.com"
        name = "Peter Administrator"
        role = UserAccount.Role.ADMIN
      }

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
          addLlmProvider {
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

        addAiPlaygroundResult {
          this.user = organizationMember.self
          this.language = czech.self
          this.project = this@build.self
          this.key = keys.get(0).self
        }

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
