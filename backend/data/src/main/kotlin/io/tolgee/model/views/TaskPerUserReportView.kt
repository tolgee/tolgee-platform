package io.tolgee.model.views

import io.tolgee.model.UserAccount

interface TaskPerUserReportView {
  val user: UserAccount
  val doneItems: Long
  val baseCharacterCount: Long
  val baseWordCount: Long
}
