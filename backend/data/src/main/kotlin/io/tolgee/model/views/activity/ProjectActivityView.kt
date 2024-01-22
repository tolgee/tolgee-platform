package io.tolgee.model.views.activity

import io.tolgee.activity.data.ActivityType

class ProjectActivityView(
  val revisionId: Long,
  val timestamp: Long,
  val type: ActivityType,
  var authorId: Long? = null,
  var authorName: String? = null,
  var authorUsername: String? = null,
  var authorAvatarHash: String? = null,
  val authorDeleted: Boolean = false,
  var modifications: List<ModifiedEntityView>? = null,
  val meta: Map<String, Any?>? = null,
  val counts: MutableMap<String, Long>? = null,
  val params: Any? = null,
)
