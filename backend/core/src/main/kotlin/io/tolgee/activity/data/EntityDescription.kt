package io.tolgee.activity.data

import java.io.Serializable

open class EntityDescription(
  val entityClass: String,
  val entityId: Long,
  val data: Map<String, Any?>,
) : Serializable
