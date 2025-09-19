package io.tolgee.ee.api.v2.controllers.branching

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchTranslationsTestData
import io.tolgee.ee.repository.BranchRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.servlet.ResultActions
import kotlin.system.measureTimeMillis

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class BranchCopyIntegrationTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: BranchTranslationsTestData

  @Autowired
  lateinit var branchRepository: BranchRepository

  @BeforeEach
  fun setup() {
    testData = BranchTranslationsTestData()
    projectSupplier = { testData.project }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `copies keys and translations to new branch`() {
    val projectId = testData.project.id

    performBranchCreation().andIsOk.andAssertThatJson {
      node("name").isEqualTo("feature-x")
      node("active").isEqualTo(true)
    }

    val newBranchId = branchRepository.findByProjectIdAndName(projectId, "feature-x")!!.id
    val newBranchKeyCount = keyRepository.countByProjectAndBranch(projectId, newBranchId)

    newBranchKeyCount.assert.isEqualTo(500)

    // branch should be ready
    val branch = branchRepository.findByIdOrNull(newBranchId)!!
    branch.pending.assert.isFalse()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `copying a lot data is not slow`() {
    val data = testData.generateBunchData(2000)
    testDataService.saveTestData { data.build {} }
    var response: ResultActions
    val time = measureTimeMillis {
      response = performBranchCreation()
    }
    response.andIsOk
    time.assert.isLessThan(3000)
  }

  private fun performBranchCreation(): ResultActions {
    return performProjectAuthPost(
      "branches",
      mapOf(
        "name" to "feature-x",
        "originBranchId" to testData.mainBranch.id,
      )
    )
  }
}
