package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.ApiKey
import io.tolgee.model.Organization
import io.tolgee.model.Pat
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope

class ImplicitUserLegacyData : BaseTestData() {
  lateinit var implicitUserAccount: UserAccount
  lateinit var randomUser: UserAccount
  lateinit var coolOrganization: Organization
  lateinit var coolProject: Project
  lateinit var randomOrganization: Organization
  lateinit var randomProject: Project
  lateinit var pat1: Pat
  lateinit var pat2: Pat
  lateinit var pak: ApiKey

  init {
    root.apply {
      addUserAccount {
        username = "___implicit_user"
        implicitUserAccount = this
      }.build {
        addOrganization {
          name = "cool-org"
          coolOrganization = this
        }.build {
          addRole {
            user = implicitUserAccount
            type = OrganizationRoleType.OWNER
          }

          addProject {
            name = "cool-project"
            coolProject = this
          }.build {
            addApiKey {
              pak = this
              userAccount = implicitUserAccount
            }
          }
        }

        addPat {
          description = "pat 1"
          userAccount = implicitUserAccount
          pat1 = this
        }

        addPat {
          description = "pat 2"
          userAccount = implicitUserAccount
          pat2 = this
        }
      }

      addUserAccount {
        username = "random-person"
        randomUser = this
      }.build {
        addOrganization {
          name = "random-org"
          randomOrganization = this
        }.build {
          addRole {
            user = randomUser
            type = OrganizationRoleType.OWNER
          }

          addProject {
            name = "random-project"
            randomProject = this
          }.build {
            addPermission {
              user = implicitUserAccount
              scopes = Scope.expand(Scope.SCREENSHOTS_UPLOAD)
              type = null
            }
          }
        }
      }
    }
  }
}
