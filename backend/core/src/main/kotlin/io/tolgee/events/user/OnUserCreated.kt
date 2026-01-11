package io.tolgee.events.user

import io.tolgee.model.UserAccount

class OnUserCreated(
  source: Any,
  userAccount: UserAccount,
  userSource: String? = null,
) : UserAccountEvent(source, userAccount, userSource)
