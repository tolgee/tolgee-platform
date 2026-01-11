package io.tolgee.model.activity

data class ActivityModifiedEntityId(
  var activityRevision: Long? = null,
  var entityClass: String? = null,
  var entityId: Long? = null,
) : java.io.Serializable
