package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.ApiKey
import io.tolgee.model.Pat
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import java.util.Date

class ApiKeysTestData : BaseTestData() {
  lateinit var frantisekDobrota: UserAccount
  lateinit var frantasProject: Project
  lateinit var frantasKey: ApiKey
  lateinit var frantasPat: Pat
  lateinit var usersKey: ApiKey
  lateinit var expiredKey: ApiKey

  lateinit var frantasLocalizationKey: Key
  lateinit var frantasTranslation: Translation
  lateinit var usersKeyFrantasProject: ApiKey

  init {
    this.root.apply {
      val userAccountBuilder =
        addUserAccount {
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
            scopesEnum = Scope.values().toMutableSet()
            userAccount = frantisekDobrota
          }
        }

        addPermission {
          user = frantisekDobrota
          type = ProjectPermissionType.MANAGE
        }

        addPermission {
          user = this@ApiKeysTestData.user
          type = ProjectPermissionType.MANAGE
        }

        addApiKey {
          key = "test_api_key_user"
          scopesEnum = Scope.values().toMutableSet()
          userAccount = this@ApiKeysTestData.user
          usersKeyFrantasProject = this
        }

        addKey {
          name = "test-key"
          frantasLocalizationKey = this
        }.build {
          addTranslation {
            text = "test translation"
            language = addEnglish().self
            frantasTranslation = this
          }
        }
      }
    }

    this.projectBuilder.apply {
      addApiKey {
        key = "test_api_key_yep"
        scopesEnum = Scope.values().toMutableSet()
      }
      addPermission {
        user = frantisekDobrota
        type = ProjectPermissionType.TRANSLATE
      }
      addApiKey {
        key = "test_api_key_1"
        scopesEnum = mutableSetOf(Scope.TRANSLATIONS_VIEW)
        userAccount = frantisekDobrota
        frantasKey = this
      }
      addApiKey {
        key = "test_api_key_2"
        scopesEnum = Scope.values().toMutableSet()
        usersKey = this
      }
      addApiKey {
        key = "expired_key"
        scopesEnum = Scope.values().toMutableSet()
        expiresAt = Date(1661242685000)
        description = "Oh I am expired"
        lastUsedAt = Date(1661342685000)
        expiredKey = this
      }
    }
  }
}
