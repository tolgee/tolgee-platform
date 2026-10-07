package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope

/**
 * The default project a socket subscribes to. Each subscription refusal needs a different second party, so they are
 * added by the test that needs one rather than carried by every test in the class.
 */
class WebsocketAuthenticationTestData : BaseTestData() {
  /** A second account, to be refused a subscription to a project it holds no permission on. */
  fun addSecondUser(): UserAccountBuilder = root.addUserAccount { username = "user2" }

  fun makeUserInitial() {
    user.isInitialUser = true
  }

  fun addProjectApiKey(key: String) {
    projectBuilder.addApiKey {
      this.key = key
      scopesEnum = mutableSetOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW)
      userAccount = this@WebsocketAuthenticationTestData.user
    }
  }

  /** A second project in the same organization, so a key bound to it can be aimed at the first one. */
  fun addOtherProject(): Project =
    root
      .addProject {
        name = "websocket_other_project"
        organizationOwner = projectBuilder.self.organizationOwner
      }.self

  /** An admin with no membership on the default project, so a key of theirs must not inherit the elevation. */
  fun addOutsideAdmin(): UserAccount =
    root
      .addUserAccount {
        username = "websocket_outside_admin"
        role = UserAccount.Role.ADMIN
      }.self
}
