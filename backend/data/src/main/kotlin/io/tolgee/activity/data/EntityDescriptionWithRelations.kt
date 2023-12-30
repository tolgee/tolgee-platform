package io.tolgee.activity.data

open class EntityDescriptionWithRelations(
  entityClass: String,
  entityId: Long,
  data: Map<String, Any?>,
  val relations: Map<String, EntityDescriptionWithRelations>,
) : EntityDescription(
    entityClass,
    entityId,
    data,
  )
