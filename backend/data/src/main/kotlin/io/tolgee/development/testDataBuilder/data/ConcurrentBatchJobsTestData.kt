package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

/**
 * Test data for concurrent batch job integration tests.
 * Creates multiple projects with different amounts of data to test
 * concurrent job execution across projects.
 */
class ConcurrentBatchJobsTestData(
  val projectAKeyCount: Int = 500,
  val projectBKeyCount: Int = 300,
  val projectCKeyCount: Int = 200,
) {
  lateinit var user: UserAccount
  lateinit var userAccountBuilder: UserAccountBuilder

  lateinit var projectA: Project
  lateinit var projectABuilder: ProjectBuilder
  lateinit var projectAEnglish: Language
  lateinit var projectACzech: Language
  lateinit var projectAGerman: Language

  lateinit var projectB: Project
  lateinit var projectBBuilder: ProjectBuilder
  lateinit var projectBEnglish: Language
  lateinit var projectBCzech: Language

  lateinit var projectC: Project
  lateinit var projectCBuilder: ProjectBuilder
  lateinit var projectCEnglish: Language
  lateinit var projectCCzech: Language

  val root: TestDataBuilder =
    TestDataBuilder().apply {
      userAccountBuilder =
        addUserAccount {
          username = "concurrent_batch_test_user"
        }

      user = userAccountBuilder.self

      // Project A - largest project for main testing (MT, pre-translate)
      projectABuilder =
        addProject {
          name = "project_a"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build buildProjectA@{
          addPermission {
            project = this@buildProjectA.self
            user = this@ConcurrentBatchJobsTestData.user
            type = ProjectPermissionType.MANAGE
          }

          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            projectAEnglish = this
            this@buildProjectA.self.baseLanguage = this
          }

          addLanguage {
            name = "Czech"
            tag = "cs"
            originalName = "Čeština"
            projectACzech = this
          }

          addLanguage {
            name = "German"
            tag = "de"
            originalName = "Deutsch"
            projectAGerman = this
          }

          this.self {
            baseLanguage = projectAEnglish
          }

          projectA = this@buildProjectA.self
        }

      // Project B - medium project for competing jobs
      projectBBuilder =
        addProject {
          name = "project_b"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build buildProjectB@{
          addPermission {
            project = this@buildProjectB.self
            user = this@ConcurrentBatchJobsTestData.user
            type = ProjectPermissionType.MANAGE
          }

          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            projectBEnglish = this
            this@buildProjectB.self.baseLanguage = this
          }

          addLanguage {
            name = "Czech"
            tag = "cs"
            originalName = "Čeština"
            projectBCzech = this
          }

          this.self {
            baseLanguage = projectBEnglish
          }

          projectB = this@buildProjectB.self
        }

      // Project C - smaller project for non-exclusive job testing
      projectCBuilder =
        addProject {
          name = "project_c"
          organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self
        }.build buildProjectC@{
          addPermission {
            project = this@buildProjectC.self
            user = this@ConcurrentBatchJobsTestData.user
            type = ProjectPermissionType.MANAGE
          }

          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            projectCEnglish = this
            this@buildProjectC.self.baseLanguage = this
          }

          addLanguage {
            name = "Czech"
            tag = "cs"
            originalName = "Čeština"
            projectCCzech = this
          }

          this.self {
            baseLanguage = projectCEnglish
          }

          projectC = this@buildProjectC.self
        }
    }

  fun populateProjectA() {
    projectABuilder.apply {
      (1..projectAKeyCount).forEach { i ->
        addKey {
          name = "key_a_$i"
        }.build {
          addTranslation {
            language = projectAEnglish
            text = "English text A $i"
          }
        }
      }
    }
  }

  fun populateProjectB() {
    projectBBuilder.apply {
      (1..projectBKeyCount).forEach { i ->
        addKey {
          name = "key_b_$i"
        }.build {
          addTranslation {
            language = projectBEnglish
            text = "English text B $i"
          }
        }
      }
    }
  }

  fun populateProjectC() {
    projectCBuilder.apply {
      (1..projectCKeyCount).forEach { i ->
        addKey {
          name = "key_c_$i"
        }.build {
          addTranslation {
            language = projectCEnglish
            text = "English text C $i"
          }
        }
      }
    }
  }

  fun populateAllProjects() {
    populateProjectA()
    populateProjectB()
    populateProjectC()
  }

  fun getProjectAKeyIds(): List<Long> = projectABuilder.data.keys.map { it.self.id }

  fun getProjectBKeyIds(): List<Long> = projectBBuilder.data.keys.map { it.self.id }

  fun getProjectCKeyIds(): List<Long> = projectCBuilder.data.keys.map { it.self.id }
}
