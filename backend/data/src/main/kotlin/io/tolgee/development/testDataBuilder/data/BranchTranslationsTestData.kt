package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.ProjectPermissionType

class BranchTranslationsTestData {
  lateinit var project: Project
  lateinit var en: Language
  lateinit var de: Language
  lateinit var user: UserAccount
  lateinit var mainBranch: Branch

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
        mainBranch = addBranch {
          name = "main"
          project = this@project.self
          isDefault = true
          isProtected = true
        }.build {
          (1..500).forEach {
            this@project.addBranchKey(it, "branched key", this@build.self)
          }
        }.self
      }.self
    }

  fun generateBunchData(n: Int): ProjectBuilder {
    return root.data.projects[0].apply {
      (1..n).forEach {
        addBranchKey(it, "branched additional key", mainBranch)
      }
    }
  }

  private fun ProjectBuilder.addBranchKey(num: Int, prefix: String, branch: Branch) {
    addKey {
      name = "$prefix $num"
      this.branch = branch
    }.build {
      addTranslation {
        language = en
        text = "I am key number $num - english"
      }
      addTranslation {
        language = de
        text = "I am key number $num - german"
      }
    }
  }
}
