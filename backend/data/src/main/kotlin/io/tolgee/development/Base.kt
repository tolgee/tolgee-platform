package io.tolgee.development

import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.UserAccount

data class Base(
  val project: Project,
  var organization: Organization,
  val userAccount: UserAccount,
)
