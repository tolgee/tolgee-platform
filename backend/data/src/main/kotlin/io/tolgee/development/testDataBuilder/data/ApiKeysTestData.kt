package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.ApiKey
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope

class ApiKeysTestData : BaseTestData() {
  lateinit var frantisekDobrota: UserAccount
  lateinit var frantasProject: Project
  lateinit var frantasKey: ApiKey
  lateinit var usersKey: ApiKey

  init {
    this.root.apply {
      addUserAccount {
        name = "Franta Dobrota"
        username = "franta"
        frantisekDobrota = this
      }

      addProject {
        userOwner = frantisekDobrota
        name = "Frantisek's project"
        frantasProject = this
      }.build {
        (0..100).forEach {
          addApiKey {
            key = "test_api_key_franta_$it"
            scopesEnum = ApiScope.values().toMutableSet()
            userAccount = frantisekDobrota
          }
        }

        addPermission {
          user = frantisekDobrota
          type = Permission.ProjectPermissionType.MANAGE
        }

        addPermission {
          user = this@ApiKeysTestData.user
          type = Permission.ProjectPermissionType.MANAGE
        }
      }
    }

    this.projectBuilder.apply {
      addApiKey {
        key = "test_api_key_yep"
        scopesEnum = ApiScope.values().toMutableSet()
      }
      addPermission {
        user = frantisekDobrota
        type = Permission.ProjectPermissionType.TRANSLATE
      }
      addApiKey {
        key = "test_api_key_1"
        scopesEnum = mutableSetOf(ApiScope.TRANSLATIONS_VIEW)
        userAccount = frantisekDobrota
        frantasKey = this
      }
      addApiKey {
        key = "test_api_key_2"
        scopesEnum = ApiScope.values().toMutableSet()
        usersKey = this
      }
    }
  }
}
