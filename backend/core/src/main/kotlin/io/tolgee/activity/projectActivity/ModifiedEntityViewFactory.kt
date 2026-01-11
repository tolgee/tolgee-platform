package io.tolgee.activity.projectActivity

import io.sentry.Sentry
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.views.activity.ModifiedEntityView

class ModifiedEntityViewFactory(
  private val entityExistences: Map<Pair<String, Long>, Boolean>,
  private val allRelationData: MutableMap<Long, MutableList<ActivityDescribingEntity>>,
) {
  fun create(modifiedEntity: ActivityModifiedEntity): ModifiedEntityView {
    val relations =
      modifiedEntity.describingRelations
        ?.mapNotNull relationsOfEntityMap@{ relationEntry ->
          relationEntry.key to
            extractCompressedRef(
              relationEntry.value,
              allRelationData[modifiedEntity.activityRevision.id] ?: let {
                Sentry.captureException(
                  IllegalStateException("No relation data for revision ${modifiedEntity.activityRevision.id}"),
                )
                return@relationsOfEntityMap null
              },
            )
        }?.toMap()

    return ModifiedEntityView(
      entityClass = modifiedEntity.entityClass,
      entityId = modifiedEntity.entityId,
      exists = entityExistences[modifiedEntity.entityClass to modifiedEntity.entityId],
      modifications = modifiedEntity.modifications,
      description = modifiedEntity.describingData,
      describingRelations = relations,
    )
  }

  private fun extractCompressedRef(
    value: EntityDescriptionRef,
    describingEntities: List<ActivityDescribingEntity>,
  ): ExistenceEntityDescription {
    return CompressedRefExtractor(entityExistences).extract(value, describingEntities)
  }
}
