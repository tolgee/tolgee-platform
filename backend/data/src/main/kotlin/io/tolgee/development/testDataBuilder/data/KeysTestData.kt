package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag

class KeysTestData {
  lateinit var keyWithReferences: Key
  lateinit var project: Project
  var project2: Project
  var user: UserAccount
  lateinit var firstKey: Key
  lateinit var secondKey: Key
  lateinit var english: Language
  lateinit var german: Language

  val root: TestDataBuilder = TestDataBuilder().apply {
    user = addUserAccount {
      username = "Peter"
    }.self

    project2 = addProject {
      name = "Other project"
      userOwner = user
    }.build {
      addPermission {
        user = this@KeysTestData.user
        type = Permission.ProjectPermissionType.MANAGE
      }
    }.self

    addProject {
      name = "Peter's project"
      userOwner = user
      project = this
    }.build {
      english = addLanguage {

        name = "English"
        tag = "en"
      }.self

      german = addLanguage {

        name = "German"
        tag = "de"
      }.self

      addPermission {

        user = this@KeysTestData.user
        type = Permission.ProjectPermissionType.MANAGE
      }

      firstKey = addKey {
        name = "first_key"
      }.self

      secondKey = addKey {
        name = "second_key"
      }.self

      addKey {
        name = "key_with_referecnces"
        this@KeysTestData.keyWithReferences = this
      }.build {
        addScreenshot {}
        addMeta {
          tags.add(
            Tag().apply {
              project = projectBuilder.self
              name = "test"
            }
          )
          addComment {
            text = "What a text comment"
          }
          addCodeReference {
            line = 20
            path = "./code/exist.extension"
          }
        }
      }
    }
  }
}
