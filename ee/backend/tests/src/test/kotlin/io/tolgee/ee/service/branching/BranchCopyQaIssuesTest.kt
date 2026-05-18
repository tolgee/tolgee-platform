package io.tolgee.ee.service.branching

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.development.testDataBuilder.data.BranchCopyQaIssuesTestData
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.branching.BranchService
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Verifies that creating a branch copies the source branch's QA issues forward and
 * preserves each translation's `qa_checks_stale` flag, so the new branch does not
 * need a redundant QA recheck (translations are identical to the source).
 */
@SpringBootTest
class BranchCopyQaIssuesTest : AbstractSpringTest() {
  @Autowired
  lateinit var branchService: BranchService

  @Autowired
  lateinit var qaIssueRepository: TranslationQaIssueRepository

  @Autowired
  lateinit var batchJobService: BatchJobService

  private lateinit var testData: BranchCopyQaIssuesTestData

  @BeforeEach
  fun setup() {
    testData = BranchCopyQaIssuesTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `copies QA issues to new branch and preserves qa_checks_stale`() {
    val sourceTranslationId = testData.englishTranslation.id
    val sourceKey = testData.key

    // Act: create a new branch from the default branch.
    val newBranch =
      branchService.createBranch(
        projectId = testData.project.id,
        name = "feature-x",
        originBranchId = testData.defaultBranch.id,
        author = testData.user,
      )

    // Assert: new branch translation got both QA issues, with correct fields and states.
    executeInNewTransaction(platformTransactionManager) {
      val targetTranslation = findTranslationOnBranch(newBranch, sourceKey.name)
      val copiedIssues = qaIssueRepository.findAllByTranslationId(targetTranslation.id)
      copiedIssues.assert.hasSize(2)

      val open = copiedIssues.single { it.type == QaCheckType.PUNCTUATION_MISMATCH }
      open.state.assert.isEqualTo(QaIssueState.OPEN)
      open.virtual.assert.isFalse()
      open.translation.id.assert
        .isEqualTo(targetTranslation.id)

      val virtual = copiedIssues.single { it.type == QaCheckType.CHARACTER_CASE_MISMATCH }
      virtual.state.assert.isEqualTo(QaIssueState.IGNORED)
      virtual.virtual.assert.isTrue()

      // qa_checks_stale carried over (not reset to DB default of `true`).
      targetTranslation.qaChecksStale.assert.isFalse()

      // Source's issues are unchanged (we copied, not moved).
      val sourceIssues = qaIssueRepository.findAllByTranslationId(sourceTranslationId)
      sourceIssues.assert.hasSize(2)
    }
  }

  private fun findTranslationOnBranch(
    branch: Branch,
    keyName: String,
  ): Translation {
    val key =
      entityManager
        .createQuery(
          "select k from Key k where k.project.id = :pid and k.branch.id = :bid and k.name = :name",
          Key::class.java,
        ).setParameter("pid", testData.project.id)
        .setParameter("bid", branch.id)
        .setParameter("name", keyName)
        .singleResult
    return entityManager
      .createQuery(
        "select t from Translation t where t.key.id = :kid and t.language.id = :lid",
        Translation::class.java,
      ).setParameter("kid", key.id)
      .setParameter("lid", testData.englishLanguage.id)
      .singleResult
  }
}
