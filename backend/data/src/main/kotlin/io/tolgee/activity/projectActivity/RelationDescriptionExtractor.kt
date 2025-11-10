package io.tolgee.activity.projectActivity

import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.EntityDescriptionWithRelations
import io.tolgee.model.activity.ActivityDescribingEntity
import org.springframework.stereotype.Component

@Component
class RelationDescriptionExtractor {
  fun extract(
    value: EntityDescriptionRef,
    describingEntities: List<ActivityDescribingEntity>,
  ): EntityDescriptionWithRelations {
    val entity = describingEntities.find { it.entityClass == value.entityClass && it.entityId == value.entityId }

    val relations =
      entity
        ?.describingRelations
        ?.map { it.key to extract(it.value, describingEntities) }
        ?.toMap()

    return EntityDescriptionWithRelations(
      entityClass = value.entityClass,
      entityId = value.entityId,
      data = entity?.data ?: mapOf(),
      relations = relations ?: mapOf(),
    )
  }
}
