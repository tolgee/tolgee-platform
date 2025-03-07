package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.AuthProviderChangeRequest

class AuthProviderChangeRequestBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<AuthProviderChangeRequest, AuthProviderChangeRequestBuilder> {
  override var self: AuthProviderChangeRequest =
    AuthProviderChangeRequest().apply {
      userAccount = userAccountBuilder.self
      userAccountBuilder.self.authProviderChangeRequest = this
    }
}
