package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.ee.service.branching.BranchSnapshotService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/branch-merge"])
class BranchMergeE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  private var currentTestData: BranchMergeTestData? = null

  @Autowired
  private lateinit var branchSnapshotService: BranchSnapshotService

  override val testData: TestDataBuilder
    get() {
      currentTestData = BranchMergeTestData()
      return currentTestData!!.root
    }

  override fun afterTestDataStored(data: TestDataBuilder) {
    val testData = currentTestData ?: return
    createInitialSnapshot(testData)
  }

  private fun createInitialSnapshot(testData: BranchMergeTestData) {
    branchSnapshotService.createInitialSnapshot(testData.project.id, testData.mainBranch, testData.featureBranch)
  }
}
