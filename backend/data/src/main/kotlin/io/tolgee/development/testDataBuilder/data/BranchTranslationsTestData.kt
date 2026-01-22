package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Key
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Label

class BranchTranslationsTestData {
  lateinit var project: Project
  lateinit var en: Language
  lateinit var de: Language
  lateinit var user: UserAccount
  lateinit var mainBranch: Branch
  lateinit var toBeDeletedBranch: Branch
  lateinit var firstLabel: Label
  lateinit var secondLabel: Label
  lateinit var thirdLabel: Label
  lateinit var firstTag: Tag
  lateinit var secondTag: Tag
  lateinit var protectedBranch: Branch
  lateinit var protectedKey: Key
  lateinit var branchedKey: Key
  lateinit var branchedScreenshotReference: KeyScreenshotReference
  lateinit var protectedScreenshotReference: KeyScreenshotReference

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      val userAccountBuilder =
        addUserAccount {
          username = "ye"
          user = this
        }
      addProject {
        name = "Branch project"
        organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        project = this
        useBranching = true
      }.build project@{
        addPermission {
          user = this@BranchTranslationsTestData.user
          type = ProjectPermissionType.MANAGE
        }
        en = addEnglish().self
        de = addGerman().self
        firstTag =
          Tag().apply {
            name = "draft"
            project = this@project.self
          }
        secondTag =
          Tag().apply {
            name = "feature"
            project = this@project.self
          }
        firstLabel =
          addLabel {
            name = "first"
            color = "#FF5555"
          }.self
        secondLabel =
          addLabel {
            name = "wohoo"
            color = "#FF0000"
          }.self
        thirdLabel =
          addLabel {
            name = "unassigned"
            color = "#00FF00"
          }.self
        mainBranch =
          addBranch {
            name = "main"
            project = this@project.self
            isDefault = true
            isProtected = false
          }.build {
            branchedKey =
              this@project
                .addBranchKey(0, "branched key example", this@build.self)
                .build {
                  val screenshot = addScreenshot { _ -> }.self
                  branchedScreenshotReference =
                    addScreenshotReference {
                      key = self
                      this.screenshot = screenshot
                    }.self
                }.self
            (1..50).forEach {
              this@project.addBranchKey(it, "branched key", this@build.self)
            }
          }.self
        protectedBranch =
          addBranch {
            name = "protected"
            project = this@project.self
            isProtected = true
          }.build {
            protectedKey =
              addKey {
                name = "protected-key"
                branch = this@build.self
              }.build {
                addMeta {
                  description = "description of protected key"
                  tags.add(firstTag)
                  tags.add(secondTag)
                }
                val screenshot = addScreenshot { _ -> }.self
                protectedScreenshotReference =
                  addScreenshotReference {
                    key = self
                    this.screenshot = screenshot
                  }.self
                addTranslation {
                  language = de
                  text = "Branched german key."
                }
                addTranslation {
                  language = en
                  text = "Branched english key."
                  labels = mutableSetOf(firstLabel)
                }
              }.self
          }.self
      }.self
    }

  fun generateBunchData(
    n: Int,
    branch: Branch = mainBranch,
  ): ProjectBuilder {
    return root.data.projects[0].apply {
      (1..n).forEach {
        addBranchKey(it, "branched additional key", branch)
      }
    }
  }

  fun addBranchToBeDeleted(name: String = "to-be-deleted"): ProjectBuilder {
    return root.data.projects[0].apply {
      addBranch {
        this.name = name
        project = root.data.projects[0].self
      }.build {
        toBeDeletedBranch = self
        (1..500).forEach {
          addBranchKey(it, "branched key to delete", this@build.self)
        }
      }
    }
  }

  private fun ProjectBuilder.addBranchKey(
    num: Int,
    prefix: String,
    branch: Branch,
  ): KeyBuilder {
    return addKey {
      name = "$prefix $num"
      this.branch = branch
    }.build {
      addMeta {
        description = "description of key number $num"
        tags.add(firstTag)
        tags.add(secondTag)
        addComment {
          text = "text comment"
        }
        addCodeReference {
          line = 20
          path = "./code/exist.extension"
          author = user
        }
      }.build {
        addScreenshot { }
      }
      addTranslation {
        language = en
        text = "I am key number $num - english"
        labels = mutableSetOf(firstLabel, secondLabel)
      }.build {
        addComment {
          text = "comment $num"
        }
      }
      addTranslation {
        language = de
        text = "I am key number $num - german"
        labels = mutableSetOf(secondLabel)
      }
    }
  }
}
