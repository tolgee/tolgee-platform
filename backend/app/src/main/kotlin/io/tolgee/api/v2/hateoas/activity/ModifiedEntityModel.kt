package io.tolgee.api.v2.hateoas.activity

import io.tolgee.activity.EntityDescription
import io.tolgee.activity.PropertyModification

data class ModifiedEntityModel(
  val entityId: Long,
  val description: Map<String, Any?>? = null,
  var modifications: Map<String, PropertyModification>? = null,
  var relations: Map<String, EntityDescription>? = null,
  val exists: Boolean? = null
)
