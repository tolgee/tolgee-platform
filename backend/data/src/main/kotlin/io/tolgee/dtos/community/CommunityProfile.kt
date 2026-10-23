package io.tolgee.dtos.community

import io.tolgee.dtos.Avatar

data class CommunityProfile(
  val id: Long,
  val name: String?,
  val avatar: Avatar?,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
  val languages: List<ProfileLanguage>,
  val projects: List<ProfileProject>,
  val days: List<DayCounts>,
)
