package io.tolgee.api.v2.hateoas.project

import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel

open class ProjectStatsModel(
  val projectId: Int,
  val languageCount: Int,
  val keyCount: Int,
  val baseWordsCount: Int,
  val translatedPercent: Int,
  val reviewedPercent: Int,
  val membersCount: Int,
  val userOwner: UserAccountModel?,
  val organizationOwner: OrganizationModel?,
  val tagCount: Int,
)
