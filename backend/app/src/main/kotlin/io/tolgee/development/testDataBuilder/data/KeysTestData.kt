package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag

class KeysTestData {
  lateinit var keyWithReferences: Key
  var project: Project
  var project2: Project
  var user: UserAccount
  lateinit var firstKey: Key
  lateinit var secondKey: Key
  lateinit var english: Language
  lateinit var german: Language

  val root: TestDataBuilder = TestDataBuilder().apply {
    user = addUserAccount {
      self {
        username = "Peter"
      }
    }.self

    project2 = addProject {
      self {
        name = "Other project"
        userOwner = user
      }
      addPermission {
        self {
          project = this@addProject.self
          user = this@KeysTestData.user
          type = Permission.ProjectPermissionType.MANAGE
        }
      }
    }.self

    project = addProject {
      self {
        name = "Peter's project"
        userOwner = user
      }

      english = addLanguage {
        self {
          name = "English"
          tag = "en"
        }
      }.self

      german = addLanguage {
        self {
          name = "German"
          tag = "de"
        }
      }.self

      addPermission {
        self {
          project = this@addProject.self
          user = this@KeysTestData.user
          type = Permission.ProjectPermissionType.MANAGE
        }
      }

      firstKey = addKey {
        self.name = "first_key"
      }.self

      secondKey = addKey {
        self.name = "second_key"
      }.self

      keyWithReferences = addKey {
        self.name = "key_with_referecnces"
        addScreenshot {}
        addMeta {
          self {
            tags.add(
              Tag().apply {
                project = projectBuilder.self
                name = "test"
              }
            )
            addComment {
              self {
                text = "What a text comment"
              }
            }
            addCodeReference {
              self {
                line = 20
                path = "./code/exist.extension"
              }
            }
          }
        }
      }.self
    }.self
  }
}
