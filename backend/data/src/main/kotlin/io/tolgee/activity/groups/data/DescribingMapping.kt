package io.tolgee.activity.groups.data

import io.tolgee.model.StandardAuditModel
import kotlin.reflect.KClass

data class DescribingMapping(
  val entityClass: KClass<out StandardAuditModel>,
  val field: String,
)
