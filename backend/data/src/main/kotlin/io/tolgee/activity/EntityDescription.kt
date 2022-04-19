package io.tolgee.activity

data class EntityDescription(
  val entityClass: String,
  val entityId: Long,
  val data: Map<String, Any?>,
  val relations: Map<String, EntityDescription>,
  val exists: Boolean? = null
) : java.io.Serializable
