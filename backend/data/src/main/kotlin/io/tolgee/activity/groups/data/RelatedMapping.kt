package io.tolgee.activity.groups.data

import io.tolgee.model.StandardAuditModel
import kotlin.reflect.KClass

data class RelatedMapping(
  val entityClass: KClass<out StandardAuditModel>,
  val field: String,
  val entities: Iterable<ModifiedEntityView>,
)
