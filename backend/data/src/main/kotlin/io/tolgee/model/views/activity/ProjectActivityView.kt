package io.tolgee.model.views.activity

import io.tolgee.model.activity.ActivityModifiedEntity

class ProjectActivityView(
  val revisionId: Long,
  val timestamp: Long,
  val type: String,
  var authorId: Long? = null,
  var authorName: String? = null,
  var authorUsername: String? = null,
  var authorAvatarHash: String? = null,
  var modifications: List<ActivityModifiedEntity>? = null,
  val meta: Map<String, Any?>? = null
)
