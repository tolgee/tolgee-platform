package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import java.util.*

class ApiKeysTestData : BaseTestData() {
  lateinit var frantisekDobrota: UserAccount
  lateinit var frantasProject: Project
  lateinit var frantasKey: ApiKey
  lateinit var frantasPat: Pat
  lateinit var usersKey: ApiKey
  lateinit var expiredKey: ApiKey

  init {
    this.root.apply {
      val userAccountBuilder = addUserAccount {
        name = "Franta Dobrota"
        username = "franta"
        frantisekDobrota = this
      }.build {
        addPat {
          description = "cool pat"
          frantasPat = this
        }
      }

      addProject {
        organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
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
      addApiKey {
        key = "expired_key"
        scopesEnum = ApiScope.values().toMutableSet()
        expiresAt = Date(1661242685000)
        description = "Oh I am expired"
        lastUsedAt = Date(1661342685000)
        expiredKey = this
      }
    }
  }
}
