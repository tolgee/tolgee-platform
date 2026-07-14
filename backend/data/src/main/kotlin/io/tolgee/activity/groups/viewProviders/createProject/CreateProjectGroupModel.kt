package io.tolgee.activity.groups.viewProviders.createProject

import io.tolgee.activity.groups.baseModels.ActivityGroupLanguageModel

class CreateProjectGroupModel(
  val id: Long,
  val name: String,
  val languages: List<ActivityGroupLanguageModel>,
  val description: String?,
)
