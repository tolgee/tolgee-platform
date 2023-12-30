package io.tolgee.hateoas.quickStart

import org.springframework.hateoas.RepresentationModel

data class QuickStartModel(
  val finished: Boolean,
  val completedSteps: MutableList<String>,
  val open: Boolean,
) : RepresentationModel<QuickStartModel>()
