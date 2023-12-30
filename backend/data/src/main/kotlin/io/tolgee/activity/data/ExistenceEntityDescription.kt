package io.tolgee.activity.data

class ExistenceEntityDescription(
  entityClass: String,
  entityId: Long,
  data: Map<String, Any?>,
  relations: Map<String, ExistenceEntityDescription>,
  val exists: Boolean? = null,
) : EntityDescriptionWithRelations(entityClass, entityId, data, relations)
