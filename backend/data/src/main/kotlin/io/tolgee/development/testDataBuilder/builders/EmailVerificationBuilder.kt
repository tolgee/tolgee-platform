package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.EmailVerification

class EmailVerificationBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<EmailVerification, EmailVerificationBuilder> {
  override var self: EmailVerification =
    EmailVerification(code = "verification-code", userAccount = userAccountBuilder.self).apply {
      userAccountBuilder.self.emailVerification = this
    }
}
