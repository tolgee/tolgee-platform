package io.tolgee.events.user

import io.tolgee.model.UserAccount

class OnUserDeleted(
  source: Any,
  userAccount: UserAccount,
) : UserAccountEvent(source, userAccount)
