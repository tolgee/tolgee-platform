package io.tolgee.api.v2.hateoas.activity

import io.tolgee.activity.PropertyModification

data class ModifiedEntityModel(
  val entityId: Long,
  var modifications: Map<String, PropertyModification>? = null,
)
