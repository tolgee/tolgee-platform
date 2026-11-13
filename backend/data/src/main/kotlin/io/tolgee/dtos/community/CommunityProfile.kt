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
  val active: Long,
  val languages: List<ProfileLanguage>,
  val projects: List<ProfileProject>,
  val buckets: List<Bucket>,
)
