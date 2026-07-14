package io.tolgee.activity.rootActivity

import io.tolgee.activity.groups.matchers.modifiedEntity.DefaultMatcher
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Pageable

class RootActivityProvider(
  private val applicationContext: ApplicationContext,
  private val activityRevisionIds: List<Long>,
  private val activityTreeDefinitionItem: ActivityTreeDefinitionItem,
  private val pageable: Pageable,
  filterModifications: List<DefaultMatcher<*>>? = null,
) {
  private val filterModificationsByEntityClass = filterModifications?.groupBy { it.entityClass }

  private val rootItems by lazy {
    val rootModificationItems = filterModificationsByEntityClass?.get(activityTreeDefinitionItem.entityClass)

    RootItemsProvider(
      pageable,
      activityRevisionIds,
      activityTreeDefinitionItem.entityClass,
      rootModificationItems,
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
        ChildItemsProvider(activityRevisionIds, item, applicationContext, parentIds).provide().groupBy { it.parentId }

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
