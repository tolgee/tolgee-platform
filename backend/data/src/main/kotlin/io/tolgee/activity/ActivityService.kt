package io.tolgee.activity

import io.tolgee.model.Activity
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class ActivityService(
  private val entityManager: EntityManager
) {
  fun onActivity(activity: Activity) {
    entityManager.persist(activity.activityRevision)
    entityManager.persist(activity)
  }
}
