package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Organization
import io.tolgee.model.branching.Branch

/**
 * Test data for OrganizationStatsService.countAllWordsOnInstance scenarios.
 *
 * Two projects in two different organizations:
 *   - firstOrg: single key, EN "hello world" = 2 words.
 *   - secondOrg: use_branching = true; same key name on main branch ("one" = 1 word)
 *     and on a feature branch ("hello world there" = 3 words). MAX(1, 3) = 3 — should
 *     contribute the max, not the sum (4), to the instance-wide total.
 *
 * Expected instance-wide total contributed by this test data: 2 + 3 = 5.
 */
class CountAllWordsOnInstanceTestData {
  val root = TestDataBuilder()

  lateinit var firstOrg: Organization
  lateinit var secondOrg: Organization

  init {
    root.apply {
      addFirstOrgScenario()
      addSecondOrgScenario()
    }
  }

  private fun TestDataBuilder.addFirstOrgScenario() {
    val userBuilder =
      addUserAccount {
        username = "caw-first-org-user"
      }

    firstOrg = userBuilder.defaultOrganizationBuilder.self

    addProject {
      name = "First CAW Project"
      organizationOwner = firstOrg
      useBranching = false
    }.build {
      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      addKey { name = "caw-key1" }.build {
        addTranslation("en", "hello world")
      }
    }
  }

  private fun TestDataBuilder.addSecondOrgScenario() {
    val userBuilder =
      addUserAccount {
        username = "caw-second-org-user"
      }

    secondOrg = userBuilder.defaultOrganizationBuilder.self

    lateinit var mainBranch: Branch
    lateinit var featureBranch: Branch

    addProject {
      name = "Second CAW Project"
      organizationOwner = secondOrg
      useBranching = true
    }.build {
      addLanguage {
        name = "English"
        tag = "en"
        originalName = "English"
        this@build.self.baseLanguage = this
      }

      addBranch {
        name = "main"
        project = self
        isDefault = true
      }.build { mainBranch = self }

      addBranch {
        name = "feature"
        project = self
        originBranch = mainBranch
      }.build { featureBranch = self }

      addKey {
        name = "caw-key2"
        branch = mainBranch
      }.build {
        addTranslation("en", "one")
      }
      addKey {
        name = "caw-key2"
        branch = featureBranch
      }.build {
        addTranslation("en", "hello world there")
      }
    }
  }
}
