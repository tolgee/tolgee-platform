package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.ApiKey
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Pat
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.translation.TranslationComment

class UserDeletionTestData {
  lateinit var franta: UserAccount
  lateinit var pepa: UserAccount
  lateinit var olga: UserAccount
  lateinit var frantasPermissionInOlgasProject: Permission
  lateinit var frantasOrganization: Organization
  lateinit var frantasComment: TranslationComment
  lateinit var frantasPat: Pat
  lateinit var frantasApiKey: ApiKey
  lateinit var frantasRole: OrganizationRole
  lateinit var pepaFrantaOrganization: Organization

  val root =
    TestDataBuilder {
      addUserAccount {
        name = "Franta"
        username = "franta"
        franta = this
      }.build {
        frantasOrganization = this.defaultOrganizationBuilder.self
        addPat {
          frantasPat = this
          description = "My PAT"
        }
        setUserPreferences {
          preferredOrganization = this@build.defaultOrganizationBuilder.self
        }
      }
      addUserAccountWithoutOrganization {
        name = "Pepa"
        username = "pepa"
        pepa = this
      }
      addUserAccount {
        name = "Olga"
        username = "olga"
        olga = this
      }.build {
        this.defaultOrganizationBuilder.apply {
          addProject {
            organizationOwner = this@apply.self
            name = "Olga's project"
          }.build {
            val en = addEnglish()
            val helloKey = addKey { name = "hello" }
            addTranslation {
              language = en.self
              text = "Hello"
              key = helloKey.self
            }.build {
              addComment {
                author = franta
                frantasComment = this
                text = "Comment!"
              }
            }
            addPermission {
              user = franta
              frantasPermissionInOlgasProject = this
            }
          }
        }
      }
      addPepaAndFrantaOrganization()
    }

  private fun TestDataBuilder.addPepaAndFrantaOrganization() {
    addOrganization {
      name = "Pepa's and Franta's org"
      pepaFrantaOrganization = this
    }.build {
      addProject {
        organizationOwner = this@build.self
        name = "Project"
      }.build {
        addApiKey {
          userAccount = franta
          frantasApiKey = this
        }
      }
      addRole {
        user = pepa
        type = OrganizationRoleType.OWNER
      }
      addRole {
        user = franta
        type = OrganizationRoleType.OWNER
        frantasRole = this
      }
      addRole {
        user = olga
        type = OrganizationRoleType.MEMBER
      }
    }
  }
}
