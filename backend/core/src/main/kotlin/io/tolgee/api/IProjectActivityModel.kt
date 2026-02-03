package io.tolgee.api

import io.tolgee.activity.data.ActivityType

interface IProjectActivityModel {
  val revisionId: Long
  val timestamp: Long
  val type: ActivityType
  val author: IProjectActivityAuthorModel?
  val modifiedEntities: Map<String, List<IModifiedEntityModel>>?
  val meta: Map<String, Any?>?
  val counts: MutableMap<String, Long>?
  val params: Any?
}
