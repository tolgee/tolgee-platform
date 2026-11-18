package io.tolgee.ee.aiReview

import io.tolgee.batch.data.BatchJobType
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.SystemActor
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.util.executeInNewTransaction
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractAiReviewTest : AuthorizedControllerTest() {
  @Autowired
  protected lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  protected fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)?.also {
        it.issues.size
        it.rule?.id
      }
    }

  protected fun aiReviewJobs(
    projectId: Long,
    type: BatchJobType = BatchJobType.AI_REVIEW,
  ): List<BatchJob> =
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createQuery(
          "from BatchJob j where j.project.id = :projectId and j.type = :type",
          BatchJob::class.java,
        ).setParameter("projectId", projectId)
        .setParameter("type", type)
        .resultList
        .onEach { it.author?.id }
    }

  protected fun aiReviewerRevisions(projectId: Long): List<ActivityRevision> =
    executeInNewTransaction(platformTransactionManager) {
      entityManager
        .createQuery(
          "from ActivityRevision ar where ar.projectId = :projectId and ar.systemActor = :actor",
          ActivityRevision::class.java,
        ).setParameter("projectId", projectId)
        .setParameter("actor", SystemActor.AI_REVIEWER)
        .resultList
        .onEach { it.modifiedEntities.size }
    }
}
