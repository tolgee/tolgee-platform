package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Tag
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
  lateinit var firstTag: Tag
  lateinit var secondTag: Tag

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
      }.build project@{
        addPermission {
          user = this@BranchTranslationsTestData.user
          type = ProjectPermissionType.MANAGE
        }
        en = addEnglish().self
        de = addGerman().self
        firstTag = Tag().apply {
          name = "draft"
          project = this@project.self
        }
        secondTag = Tag().apply {
          name = "feature"
          project = this@project.self
        }
        firstLabel = addLabel {
          name = "first"
          color = "#FF5555"
        }.self
        secondLabel = addLabel {
          name = "wohoo"
          color = "#FF0000"
        }.self
        mainBranch = addBranch {
          name = "main"
          project = this@project.self
          isDefault = true
          isProtected = true
        }.build {
          (1..50).forEach {
            this@project.addBranchKey(it, "branched key", this@build.self)
          }
        }.self
      }.self
    }

  fun generateBunchData(n: Int, branch: Branch = mainBranch): ProjectBuilder {
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

  private fun ProjectBuilder.addBranchKey(num: Int, prefix: String, branch: Branch) {
    addKey {
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
