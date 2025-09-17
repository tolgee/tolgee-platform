package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchTestData
import io.tolgee.ee.repository.BranchRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.branching.Branch
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class BranchControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: BranchTestData

  @Autowired
  lateinit var branchRepository: BranchRepository

  @BeforeEach
  fun setup() {
    testData = BranchTestData(currentDateProvider)
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  /**
   * Tests that the branches are returned in the correct order (active and latest first)
   */
  @Test
  @ProjectJWTAuthTestMethod
  fun `returns list of branches`() {
    performProjectAuthGet("branches").andAssertThatJson {
      node("_embedded.branches") {
        node("[0].name").isEqualTo("feature-branch")
        node("[0].active").isEqualTo(true)
        node("[1].name").isEqualTo("main")
        node("[1].active").isEqualTo(true)
        node("[2].name").isEqualTo("merged-branch")
        node("[2].active").isEqualTo(false)
        node("[3].name").isEqualTo("merged-branch-older")
        node("[3].active").isEqualTo(false)
      }
    }
  }

  @Test()
  @ProjectJWTAuthTestMethod
  fun `creates branch`() {
    performProjectAuthPost(
      "branches",
      mapOf(
        "name" to "new-branch",
        "originBranchId" to testData.mainBranch.id,
      )
    ).andAssertThatJson {
      node("name").isEqualTo("new-branch")
      node("active").isEqualTo(true)
    }
    branchRepository.findByProjectIdAndName(testData.project.id, "new-branch").assert.isNotNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes branch`() {
    performProjectAuthDelete("branches/${testData.featureBranch.id}").andIsOk
    testData.featureBranch.refresh().archivedAt.assert.isNotNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot delete default branch`() {
    performProjectAuthDelete("branches/${testData.mainBranch.id}").andIsForbidden
    testData.mainBranch.refresh().archivedAt.assert.isNull()
  }

  private fun Branch.refresh(): Branch {
    return branchRepository.findByIdOrNull(this.id)!!
  }
}
