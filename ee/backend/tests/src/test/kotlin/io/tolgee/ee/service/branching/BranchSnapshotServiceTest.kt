package io.tolgee.ee.service.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class BranchSnapshotServiceTest : AbstractSpringTest() {
  @Autowired
  lateinit var branchSnapshotService: BranchSnapshotService

  private lateinit var testData: BranchMergeTestData

  @BeforeEach
  fun setup() {
    testData = BranchMergeTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  @Transactional
  fun `snapshot stores key meta tags`() {
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )

    val snapshot =
      branchSnapshotService
        .getSnapshotKeys(testData.featureBranch.id)
        .first { it.name == BranchMergeTestData.UPDATE_KEY_NAME }

    val snapshotTags = snapshot.keyMetaSnapshot!!.tags
    snapshotTags.assert.hasSize(3)
    snapshotTags.map { it.name }.assert.containsExactlyInAnyOrder("abc", "def", "ghi")
  }
}
