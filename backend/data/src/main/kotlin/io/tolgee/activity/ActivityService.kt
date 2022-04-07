package io.tolgee.activity

import io.tolgee.activity.holders.ActivityHolder
import io.tolgee.activity.projectActivityView.ProjectActivityViewDataProvider
import io.tolgee.model.views.activity.ProjectActivityView
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager

@Component
class ActivityService(
  private val entityManager: EntityManager,
  private val transactionManager: PlatformTransactionManager,
  private val applicationContext: ApplicationContext
) {
  fun storeActivityData(activityHolder: ActivityHolder) {
    val activityRevision = activityHolder.activityRevision ?: return
    val modifiedEntities = activityHolder.modifiedEntities

    val tt = TransactionTemplate(transactionManager)
    tt.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW

    tt.executeWithoutResult {
      entityManager.persist(activityRevision)
      activityRevision.describingRelations.forEach {
        entityManager.persist(it)
      }
      modifiedEntities.values.flatMap { it.values }.forEach { activityModifiedEntity ->
        entityManager.persist(activityModifiedEntity)
      }
    }
  }

  fun getProjectActivity(projectId: Long, pageable: Pageable): Page<ProjectActivityView>? {
    return ProjectActivityViewDataProvider(
      applicationContext = applicationContext,
      projectId = projectId,
      pageable = pageable
    ).getProjectActivity()
  }
}
