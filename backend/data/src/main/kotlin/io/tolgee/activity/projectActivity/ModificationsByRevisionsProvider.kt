package io.tolgee.activity.projectActivity

import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.views.activity.ModifiedEntityView
import io.tolgee.repository.activity.ActivityModifiedEntityRepository
import io.tolgee.service.security.UserAccountService
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class ModificationsByRevisionsProvider(
  private val applicationContext: ApplicationContext,
  private val projectId: Long,
  private val revisionIds: List<Long>,
  private val pageable: Pageable,
  private val filterEntityClass: List<String>?,
) {
  val userAccountService: UserAccountService =
    applicationContext.getBean(UserAccountService::class.java)

  private val activityModifiedEntityRepository: ActivityModifiedEntityRepository =
    applicationContext.getBean(ActivityModifiedEntityRepository::class.java)

  private val entityManager: EntityManager =
    applicationContext.getBean(EntityManager::class.java)

  private lateinit var rawModifiedEntities: Page<ActivityModifiedEntity>
  private lateinit var entityExistences: Map<Pair<String, Long>, Boolean>
  private lateinit var allRelationData: MutableMap<Long, MutableList<ActivityDescribingEntity>>

  fun get(): Page<ModifiedEntityView> {
    prepareData()
    return this.getModifiedEntities()
  }

  private fun prepareData() {
    rawModifiedEntities = getModifiedEntitiesRaw()
    allRelationData = RelationDataProvider(entityManager, rawModifiedEntities).provide()
    entityExistences = getEntityExistences()
  }

  private fun getModifiedEntities(): Page<ModifiedEntityView> {
    val factory = ModifiedEntityViewFactory(entityExistences, allRelationData)
    val content =
      rawModifiedEntities.content.map { modifiedEntity ->
        factory.create(modifiedEntity)
      }

    return PageImpl(content, rawModifiedEntities.pageable, rawModifiedEntities.totalElements)
  }

  private fun getEntityExistences(): Map<Pair<String, Long>, Boolean> {
    return EntityExistenceProvider(applicationContext, rawModifiedEntities, allRelationData).provide()
  }

  private fun getModifiedEntitiesRaw(): Page<ActivityModifiedEntity> {
    return activityModifiedEntityRepository.getModifiedEntities(projectId, revisionIds, filterEntityClass, pageable)
  }
}
