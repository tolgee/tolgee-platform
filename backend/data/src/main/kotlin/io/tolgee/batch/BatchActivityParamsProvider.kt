package io.tolgee.batch

import io.tolgee.activity.PublicParamsProvider
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class BatchActivityParamsProvider(
  private val entityManager: EntityManager
) : PublicParamsProvider {
  override fun provide(revisionIds: List<Long>): Map<Long, Any?> {
    return entityManager.createQuery(
      """select bj.activityRevision.id, bj.params from BatchJob bj 
      where bj.activityRevision.id in :revisionIds      
    """,
      Array<Any?>::class.java
    ).setParameter("revisionIds", revisionIds).resultList.associate { (it[0] as Long) to it[1] }
  }
}
