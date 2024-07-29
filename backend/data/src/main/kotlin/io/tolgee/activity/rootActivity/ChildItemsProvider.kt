package io.tolgee.activity.rootActivity

import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

class ChildItemsProvider(
  private val activityRevisionId: Long,
  private val treeItem: ActivityTreeDefinitionItem,
  private val applicationContext: ApplicationContext,
  private val parentIds: List<Long>,
) {
  fun provide(): List<ActivityTreeResultItem> {
    val rootItems = getRootItemsRaw()
    return itemsParser.parse(rootItems)
  }

  private fun getRootItemsRaw(): List<Array<Any?>> {
    return entityManager.createNativeQuery(
      """
      select entity_class, ame.describing_data, modifications, ame.entity_id id, 'AME' as type, 
      (ame.describing_relations -> :describingField -> 'entityId')::bigint as parent_id
      from activity_modified_entity ame
      where ame.entity_class = :entityClass
        and ame.activity_revision_id = :revisionId
        and (ame.describing_relations -> :describingField -> 'entityId')::bigint in :ids                            
      union
      select ade.entity_class, ade.data, null, ade.entity_id id, 'ADE' as type,
      (ade.describing_relations -> :describingField -> 'entityId')::bigint as parent_id
      from activity_describing_entity ade
      where ade.activity_revision_id = :revisionId
        and ade.entity_class = :entityClass
        and (ade.describing_relations -> :describingField -> 'entityId')::bigint in :ids
        and ade.entity_id not in (select ame.entity_id id
                                  from activity_modified_entity ame
                                  where ame.activity_revision_id = :revisionId
                                   and ame.entity_class = :entityClass
                                   and (ame.describing_relations -> :describingField -> 'entityId')::bigint in :ids 
                                  )
      order by id
    """,
      Array::class.java,
    )
      .setParameter("entityClass", treeItem.entityClass.simpleName)
      .setParameter("describingField", treeItem.describingField)
      .setParameter("revisionId", activityRevisionId)
      .setParameter("ids", parentIds)
      .resultList as List<Array<Any?>>
  }

  val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  val itemsParser: ActivityItemsParser by lazy {
    applicationContext.getBean(ActivityItemsParser::class.java)
  }
}
