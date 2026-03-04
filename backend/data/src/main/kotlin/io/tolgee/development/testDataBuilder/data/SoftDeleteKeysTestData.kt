package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

class SoftDeleteKeysTestData :
  BaseTestData(
    userName = "soft-delete-keys-user",
    projectName = "Soft delete test",
  ) {
  val czechLanguage = projectBuilder.addCzech().self
  lateinit var user2: UserAccount

  init {
    root.apply {
      val user2Builder =
        addUserAccount {
          username = "soft-delete-keys-user2"
          name = "User Two"
        }
      user2 = user2Builder.self

      projectBuilder.apply {
        addPermission {
          project = this@apply.self
          user = user2
          type = ProjectPermissionType.MANAGE
        }
      }
    }

    projectBuilder.apply {
      addKey {
        name = "key1"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Key 1 translation"
        }
      }

      addKey {
        name = "key2"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Key 2 translation"
        }
      }

      addKey {
        name = "key3"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Key 3 translation"
        }
      }

      addKey {
        name = "key4"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Key 4 translation"
        }
      }
    }
  }
}
