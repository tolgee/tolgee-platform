package io.tolgee.model.contributor

import java.io.Serializable

data class ProjectContributorId(
  var projectId: Long? = null,
  var userId: Long? = null,
) : Serializable
