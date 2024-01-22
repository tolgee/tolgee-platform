package io.tolgee.events.user

import io.tolgee.model.UserAccount

class OnUserEmailVerifiedFirst(
  source: Any,
  userAccount: UserAccount,
) : UserAccountEvent(source, userAccount)
