package io.tolgee.activity.projectActivity

import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.model.activity.ActivityDescribingEntity

class CompressedRefExtractor(
  private val entityExistences: Map<Pair<String, Long>, Boolean>,
) {
  fun extract(
    value: EntityDescriptionRef,
    describingEntities: List<ActivityDescribingEntity>,
  ): ExistenceEntityDescription {
    val entity = describingEntities.find { it.entityClass == value.entityClass && it.entityId == value.entityId }

    val relations =
      entity
        ?.describingRelations
        ?.map { it.key to extract(it.value, describingEntities) }
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
