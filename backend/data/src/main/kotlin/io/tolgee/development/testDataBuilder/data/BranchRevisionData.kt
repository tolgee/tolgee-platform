package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation

class BranchRevisionData {
  lateinit var keyWithReferences: Key
  lateinit var project: Project
  lateinit var user: UserAccount
  lateinit var english: Language
  lateinit var screenshot: Screenshot
  lateinit var tag: Tag
  lateinit var firstKey: Key
  lateinit var devBranch: Branch
  lateinit var translation: Translation
  lateinit var label: Label

  var projectBuilder: ProjectBuilder

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val userAccountBuilder =
        addUserAccount {
          username = "Peter"
          user = this
        }

      projectBuilder =
        addProject {
          name = "Branch project"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
          project = this
        }.build {
          english =
            addLanguage {
              name = "English"
              tag = "en"
            }.self

          addLanguage {
            name = "German"
            tag = "de"
          }.self

          addLabel {
            name = "test-label"
            color = "#ffffaa"
            project = this@BranchRevisionData.project
            label = this
          }

          addPermission {
            user = this@BranchRevisionData.user
            type = ProjectPermissionType.MANAGE
          }

          devBranch =
            addBranch {
              name = "dev"
            }.build {
              firstKey =
                addKey {
                  name = "first_key"
                  branch = self
                }.build {
                  translation =
                    addTranslation {
                      language = english
                      text = "first key translation"
                    }.self
                  addMeta {
                    description = "default"
                  }.build {
                    tag = addTag("main")
                  }
                  screenshot = addScreenshot { }.self
                }.self

              addKey {
                name = "key_with_references"
                branch = self
                keyWithReferences = this
              }.build {
                addTranslation {
                  language = english
                  text = "Term"
                }.self
                addScreenshotReference {
                  screenshot = this@BranchRevisionData.screenshot
                  key = self
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
                    author = user
                  }
                  custom = mutableMapOf("custom" to "value")
                }
              }
            }.self
        }
    }
}
