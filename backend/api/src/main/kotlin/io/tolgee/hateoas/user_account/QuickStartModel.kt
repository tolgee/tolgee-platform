package io.tolgee.hateoas.user_account

import org.springframework.hateoas.RepresentationModel

data class QuickStartModel(
  val open: Boolean,
  val completedSteps: MutableList<String>
) : RepresentationModel<QuickStartModel>()
