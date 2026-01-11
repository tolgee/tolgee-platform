package io.tolgee.dtos.queryResults.organization

class QuickStartView(
  override val finished: Boolean,
  override val completedSteps: Array<String>,
  override val open: Boolean,
) : IQuickStart
