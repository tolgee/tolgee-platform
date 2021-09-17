package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.ApiKey
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope

class ApiKeysTestData() : BaseTestData() {
  lateinit var frantisekDobrota: UserAccount
  lateinit var frantasProject: Project
  lateinit var frantasKey: ApiKey
  lateinit var usersKey: ApiKey

  init {
    this.root.apply {
      addUserAccount {
        self {
          name = "Franta Dobrota"
          username = "franta"
          frantisekDobrota = this
        }
      }

      addProject {
        self {
          userOwner = frantisekDobrota
          name = "Frantisek's project"
          frantasProject = this
        }

        (0..100).forEach {
          addApiKey {
            self {
              key = "test_api_key_franta_$it"
              scopesEnum = ApiScope.values().toMutableSet()
              userAccount = frantisekDobrota
            }
          }
        }

        addPermission {
          self {
            user = frantisekDobrota
            type = Permission.ProjectPermissionType.MANAGE
            project = this@addProject.self
          }
        }

        addPermission {
          self {
            user = this@ApiKeysTestData.user
            type = Permission.ProjectPermissionType.MANAGE
            project = this@addProject.self
          }
        }
      }
    }
    this.projectBuilder.apply {
      addApiKey {
        self {
          key = "test_api_key_yep"
          scopesEnum = ApiScope.values().toMutableSet()
        }
      }
      addPermission {
        self {
          user = frantisekDobrota
          type = Permission.ProjectPermissionType.TRANSLATE
        }
      }
      addApiKey {
        self {
          key = "test_api_key_1"
          scopesEnum = mutableSetOf(ApiScope.TRANSLATIONS_VIEW)
          userAccount = frantisekDobrota
          frantasKey = this
        }
      }
      addApiKey {
        self {
          key = "test_api_key_2"
          scopesEnum = ApiScope.values().toMutableSet()
          usersKey = this
        }
      }
    }
  }
}
