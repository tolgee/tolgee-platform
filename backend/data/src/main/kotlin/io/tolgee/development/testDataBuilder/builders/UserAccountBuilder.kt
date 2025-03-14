package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.development.testDataBuilder.builders.slack.SlackUserConnectionBuilder
import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.model.UserPreferences
import io.tolgee.model.notifications.Notification
import io.tolgee.model.slackIntegration.SlackUserConnection
import org.springframework.core.io.ClassPathResource

class UserAccountBuilder(
  val testDataBuilder: TestDataBuilder,
) : BaseEntityDataBuilder<UserAccount, UserAccountBuilder>() {
  var rawPassword: String? = "admin"
  override var self: UserAccount = UserAccount()
  lateinit var defaultOrganizationBuilder: OrganizationBuilder

  class DATA {
    var avatarFile: ClassPathResource? = null
    var userPreferences: UserPreferencesBuilder? = null
    var authProviderChangeRequest: AuthProviderChangeRequestBuilder? = null
    var pats: MutableList<PatBuilder> = mutableListOf()
    var slackUserConnections: MutableList<SlackUserConnectionBuilder> = mutableListOf()
    var notifications: MutableList<NotificationBuilder> = mutableListOf()
  }

  var data = DATA()

  fun setAvatar(filePath: String) {
    data.avatarFile = ClassPathResource(filePath, this.javaClass.classLoader)
  }

  fun setUserPreferences(ft: UserPreferences.() -> Unit) {
    data.userPreferences =
      UserPreferencesBuilder(this)
        .also { ft(it.self) }
  }

  fun setAuthProviderChangeRequest(ft: FT<AuthProviderChangeRequest>): AuthProviderChangeRequestBuilder {
    val builder = AuthProviderChangeRequestBuilder(this)
    ft(builder.self)
    data.authProviderChangeRequest = builder
    return builder
  }

  fun addPat(ft: FT<Pat>) = addOperation(data.pats, ft)

  fun addSlackUserConnection(ft: FT<SlackUserConnection>) = addOperation(data.slackUserConnections, ft)

  fun addNotification(ft: FT<Notification>) = addOperation(data.notifications, ft)
}
