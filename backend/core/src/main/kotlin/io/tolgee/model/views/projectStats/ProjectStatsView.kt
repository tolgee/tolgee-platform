package io.tolgee.model.views.projectStats

data class ProjectStatsView(
  val id: Long,
  val keyCount: Long,
  val memberCount: Long,
  val tagCount: Long,
  val taskCount: Long,
)
