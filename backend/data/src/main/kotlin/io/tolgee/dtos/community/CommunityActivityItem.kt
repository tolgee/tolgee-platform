package io.tolgee.dtos.community

import io.tolgee.model.Project
import io.tolgee.model.views.activity.ProjectActivityView

data class CommunityActivityItem(
  val project: Project,
  val activity: ProjectActivityView,
)
