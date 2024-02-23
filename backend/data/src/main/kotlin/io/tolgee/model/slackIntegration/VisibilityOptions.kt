package io.tolgee.model.slackIntegration

import java.util.*

enum class VisibilityOptions {
  ALL_IN_CHANNEL,
  ONLY_ME;
  val code: String
    get() = name.lowercase(Locale.getDefault())
}
