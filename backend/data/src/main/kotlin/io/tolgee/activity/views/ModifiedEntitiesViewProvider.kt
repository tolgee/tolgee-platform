/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.activity.views

import io.sentry.Sentry
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.views.activity.ModifiedEntityView
import io.tolgee.model.views.activity.SimpleModifiedEntityView
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.EntityUtil
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

class ModifiedEntitiesViewProvider(
  applicationContext: ApplicationContext,
  private val modifiedEntities: Collection<ActivityModifiedEntity>,
) {
  val userAccountService: UserAccountService =
    applicationContext.getBean(UserAccountService::class.java)

  private val activityRevisionRepository: ActivityRevisionRepository =
    applicationContext.getBean(ActivityRevisionRepository::class.java)

  private val entityManager: EntityManager =
    applicationContext.getBean(EntityManager::class.java)

  private val entityUtil: EntityUtil =
    applicationContext.getBean(EntityUtil::class.java)

  private val describingEntities: Map<Long, List<ActivityDescribingEntity>> by lazy { fetchAllowedRevisionRelations() }

  private val entityExistences: Map<Pair<String, Long>, Boolean> by lazy { fetchEntityExistences() }

  fun get(): List<ModifiedEntityView> {
    return modifiedEntities.map entities@{ entity ->
      val relations = getRelations(entity)
      ModifiedEntityView(
        entityClass = entity.entityClass,
        entityId = entity.entityId,
        exists = entityExistences[entity.entityClass to entity.entityId],
        modifications = entity.modifications,
        description = entity.describingData,
        describingRelations = relations,
      )
    }
  }

  fun getSimple(): List<SimpleModifiedEntityView> {
    return modifiedEntities.map entities@{ entity ->
      val relations = getRelations(entity)
      SimpleModifiedEntityView(
        entityClass = entity.entityClass,
        entityId = entity.entityId,
        exists = entityExistences[entity.entityClass to entity.entityId],
        modifications = entity.modifications,
        description = entity.describingData,
        describingRelations = relations,
      )
    }
  }

  private fun getRelations(entity: ActivityModifiedEntity): Map<String, ExistenceEntityDescription>? {
    return entity.describingRelations
      ?.mapNotNull {
        Pair(
          it.key,
          extractCompressedRef(
            it.value,
            describingEntities[entity.activityRevision.id] ?: let { _ ->
              Sentry.captureException(
                IllegalStateException("No relation data for revision ${entity.activityRevision.id}"),
              )
              return@mapNotNull null
            },
          ),
        )
      }
      ?.toMap()
  }

  private fun fetchAllowedRevisionRelations(): Map<Long, List<ActivityDescribingEntity>> {
    val revisionIds = modifiedEntities.map { it.activityRevision.id }
    val allowedTypes = ActivityType.entries.filter { !it.onlyCountsInList }
    return activityRevisionRepository.getRelationsForRevisions(revisionIds, allowedTypes)
      .groupBy { it.activityRevision.id }
  }

  private fun fetchEntityExistences(): Map<Pair<String, Long>, Boolean> {
    val modifiedEntityClassIdPairs = modifiedEntities.map { it.entityClass to it.entityId }
    val relationsClassIdPairs = describingEntities.flatMap { (_, data) -> data.map { it.entityClass to it.entityId } }
    val entities = (modifiedEntityClassIdPairs + relationsClassIdPairs).toHashSet()

    return entities
      .groupBy { (entityClass, _) -> entityClass }
      .mapNotNull { (entityClassName, classIdPairs) ->
        val entityClass = entityUtil.getRealEntityClass(entityClassName)
        val annotation = entityClass?.getAnnotation(ActivityReturnsExistence::class.java)
        if (annotation != null) {
          val cb = entityManager.criteriaBuilder
          val query = cb.createQuery(Long::class.java)
          val root = query.from(entityClass)
          val ids = classIdPairs.map { it.second }
          query.select(root.get("id"))
          query.where(root.get<Boolean?>("id").`in`(ids))
          val existingIds = entityManager.createQuery(query).resultList
          return@mapNotNull (entityClassName to ids.map { it to existingIds.contains(it) })
        }
        return@mapNotNull null
      }
      .flatMap { (entityClassName, existingIds) -> existingIds.map { (entityClassName to it.first) to it.second } }
      .toMap()
  }

  private fun extractCompressedRef(
    value: EntityDescriptionRef,
    describingEntities: List<ActivityDescribingEntity>,
  ): ExistenceEntityDescription {
    val entity = describingEntities.find { it.entityClass == value.entityClass && it.entityId == value.entityId }

    val relations =
      entity?.describingRelations
        ?.map { it.key to extractCompressedRef(it.value, describingEntities) }
        ?.toMap()

    return ExistenceEntityDescription(
      entityClass = value.entityClass,
      entityId = value.entityId,
      exists = entityExistences[value.entityClass to value.entityId],
      data = entity?.data ?: mapOf(),
      relations = relations ?: mapOf(),
    )
  }
}
