package io.tolgee.ee.service.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BranchSnapshotTestData
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

  private lateinit var testData: BranchSnapshotTestData

  @BeforeEach
  fun setup() {
    testData = BranchSnapshotTestData()
    testDataService.saveTestData(testData.root)
    branchSnapshotService.createInitialSnapshot(
      testData.project.id,
      testData.mainBranch,
      testData.featureBranch,
    )
  }

  @Test
  @Transactional
  fun `snapshot stores key meta tags`() {
    val snapshotTags = getSnapshotKey().keyMetaSnapshot!!.tags
    snapshotTags.assert.hasSize(3)
    snapshotTags.map { it.name }.assert.containsExactlyInAnyOrder("abc", "def", "ghi")
  }

  @Test
  @Transactional
  fun `snapshot stores translation labels`() {
    val snapshotTranslation =
      getSnapshotKey()
        .translations
        .first { it.language == "en" }

    snapshotTranslation.labels.assert.containsExactlyInAnyOrder("prod")
  }

  private fun getSnapshotKey() =
    branchSnapshotService
      .getSnapshotKeys(testData.featureBranch.id)
      .first { it.name == testData.keyToSnapshot.name }
}
