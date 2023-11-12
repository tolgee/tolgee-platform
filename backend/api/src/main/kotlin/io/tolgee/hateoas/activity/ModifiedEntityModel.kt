package io.tolgee.hateoas.activity

import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.activity.data.PropertyModification
import io.tolgee.api.IModifiedEntityModel

data class ModifiedEntityModel(
  override val entityId: Long,
  override val description: Map<String, Any?>? = null,
  override var modifications: Map<String, PropertyModification>? = null,
  override var relations: Map<String, ExistenceEntityDescription>? = null,
  override val exists: Boolean? = null
) : IModifiedEntityModel
