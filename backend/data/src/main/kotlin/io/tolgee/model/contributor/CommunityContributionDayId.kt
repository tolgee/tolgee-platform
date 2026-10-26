package io.tolgee.model.contributor

import java.io.Serializable
import java.time.LocalDate

data class CommunityContributionDayId(
  var projectId: Long? = null,
  var userId: Long? = null,
  var languageId: Long? = null,
  var day: LocalDate? = null,
) : Serializable
