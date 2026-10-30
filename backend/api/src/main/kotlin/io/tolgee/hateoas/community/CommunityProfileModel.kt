package io.tolgee.hateoas.community

import io.tolgee.dtos.Avatar
import io.tolgee.dtos.community.Bucket
import io.tolgee.dtos.community.ProfileLanguage
import io.tolgee.dtos.community.ProfileProject

@Suppress("unused")
open class CommunityProfileModel(
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
