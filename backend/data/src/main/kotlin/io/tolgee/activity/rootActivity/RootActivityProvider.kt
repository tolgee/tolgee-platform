package io.tolgee.activity.rootActivity

import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Pageable

class RootActivityProvider(
  private val applicationContext: ApplicationContext,
  private val activityRevisionId: Long,
  private val activityTreeDefinitionItem: ActivityTreeDefinitionItem,
  private val pageable: Pageable,
) {
  private val rootItems by lazy {
    RootItemsProvider(
      pageable,
      activityRevisionId,
      activityTreeDefinitionItem.entityClass,
      applicationContext,
    ).provide()
  }

  fun provide(): List<ActivityTreeResultItem> {
    addChildren(activityTreeDefinitionItem, rootItems)
    return rootItems
  }

  private fun addChildren(
    parentItemDefinition: ActivityTreeDefinitionItem,
    parentItems: List<ActivityTreeResultItem>,
  ) {
    val parentIds = parentItems.map { it.entityId }
    parentItemDefinition.children.map { (key, item) ->
      val childItems =
        ChildItemsProvider(activityRevisionId, item, applicationContext, parentIds).provide().groupBy { it.parentId }

      childItems.forEach {
        addChildren(item, it.value)
      }

      rootItems.forEach { parentItem ->
        val children =
          parentItem.children ?: let {
            parentItem.children = mutableMapOf()
            parentItem.children!!
          }

        childItems[parentItem.entityId]?.let {
          children[key] = if (item.single) it.singleOrNull() else it
        }
      }
    }
  }

  val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }
}
