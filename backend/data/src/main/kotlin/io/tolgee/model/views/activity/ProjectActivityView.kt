package io.tolgee.model.views.activity

class ProjectActivityView(
  val revisionId: Long,
  val timestamp: Long,
  val type: String,
  var authorId: Long? = null,
  var authorName: String? = null,
  var authorUsername: String? = null,
  var authorAvatarHash: String? = null,
  var modifications: List<ModifiedEntityView>? = null,
  val meta: Map<String, Any?>? = null
)
