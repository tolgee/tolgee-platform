package io.tolgee.api

import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.activity.data.PropertyModification

interface IModifiedEntityModel {
  val entityClass: String
  val entityId: Long
  val description: Map<String, Any?>?
  var modifications: Map<String, PropertyModification>?
  var relations: Map<String, ExistenceEntityDescription>?
  val exists: Boolean?
}
