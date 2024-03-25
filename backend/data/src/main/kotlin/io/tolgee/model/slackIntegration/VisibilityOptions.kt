package io.tolgee.model.slackIntegration

import java.util.*

enum class VisibilityOptions {
  ONLY_ME,
  ;

  val code: String
    get() = name.lowercase(Locale.getDefault())
}
