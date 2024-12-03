package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag

class KeysTestData {
  lateinit var enOnlyUserAccount: UserAccount
  lateinit var keyWithReferences: Key
  lateinit var project: Project
  var project2: Project
  lateinit var user: UserAccount
  lateinit var firstKey: Key
  lateinit var secondKey: Key
  lateinit var english: Language
  lateinit var german: Language
  lateinit var screenshot: Screenshot

  var projectBuilder: ProjectBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val userAccountBuilder =
        addUserAccount {
          username = "Peter"
          user = this
        }

      project2 =
        addProject {
          name = "Other project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build {
          addPermission {
            user = this@KeysTestData.user
            type = ProjectPermissionType.MANAGE
          }
        }.self

      projectBuilder =
        addProject {
          name = "Peter's project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
          project = this
        }.build {
          english =
            addLanguage {
              name = "English"
              tag = "en"
            }.self

          german =
            addLanguage {
              name = "German"
              tag = "de"
            }.self

          addPermission {
            user = this@KeysTestData.user
            type = ProjectPermissionType.MANAGE
          }

          firstKey =
            addKey {
              name = "first_key"
            }.self

          secondKey =
            addKey {
              name = "second_key"
            }.build {
              setDescription("description")
              screenshot = addScreenshot { }.self
            }.self

          addKey {
            name = "key_with_referecnces"
            this@KeysTestData.keyWithReferences = this
          }.build {
            addScreenshotReference {
              screenshot = this@KeysTestData.screenshot
              key = this@build.self
            }
            addMeta {
              tags.add(
                Tag().apply {
                  project = projectBuilder.self
                  name = "test"
                },
              )
              addComment {
                text = "What a text comment"
              }
              addCodeReference {
                line = 20
                path = "./code/exist.extension"
              }
              custom = mutableMapOf("custom" to "value")
            }
          }
        }

      addUserAccountWithoutOrganization {
        username = "enOnly"
        projectBuilder.addPermission {
          user = this@addUserAccountWithoutOrganization
          type = ProjectPermissionType.TRANSLATE
          translateLanguages = mutableSetOf(english)
        }
        enOnlyUserAccount = this
      }
    }

  fun addNKeys(n: Int) {
    (1..n).forEach {
      projectBuilder.apply {
        addKey {
          name = "key_$it"
        }
      }
    }
  }

  fun addThirdKey(): Key {
    return projectBuilder.addKey("third_key").self
  }
}
