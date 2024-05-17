package io.tolgee.activity.rootActivity

import io.tolgee.model.EntityWithId
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Pageable
import kotlin.reflect.KClass

class RootItemsProvider(
  private val pageable: Pageable,
  private val activityRevisionId: Long,
  private val rootEntityClass: KClass<out EntityWithId>,
  private val applicationContext: ApplicationContext,
) {
  fun provide(): List<ActivityTreeResultItem> {
    val rootItems = getRootItemsRaw()
    return itemsParser.parse(rootItems)
  }

  private fun getRootItemsRaw(): List<Array<Any?>> {
    val limit = pageable.pageSize
    val offset = pageable.offset
    return entityManager.createNativeQuery(
      """
      select entity_class, ame.describing_data, modifications, ame.entity_id id, 'AME' as type
      from activity_modified_entity ame
      where ame.entity_class = :entityClass
        and ame.activity_revision_id = :revisionId
      union
      select ade.entity_class, ade.data, null, ade.entity_id id, 'ADE' as type
      from activity_describing_entity ade
      where ade.activity_revision_id = :revisionId
        and ade.entity_class = :entityClass and ade.entity_id not in (
        select ame.entity_id id
          from activity_modified_entity ame
        where ame.activity_revision_id = :revisionId
          and ame.entity_class = :entityClass
        )
      order by id
      limit :limit
      offset :offset
    """,
      Array::class.java,
    )
      .setParameter("entityClass", rootEntityClass.simpleName)
      .setParameter("revisionId", activityRevisionId)
      .setParameter("limit", limit)
      .setParameter("offset", offset)
      .resultList as List<Array<Any?>>
  }

  val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  val itemsParser: ActivityItemsParser by lazy {
    applicationContext.getBean(ActivityItemsParser::class.java)
  }
}
