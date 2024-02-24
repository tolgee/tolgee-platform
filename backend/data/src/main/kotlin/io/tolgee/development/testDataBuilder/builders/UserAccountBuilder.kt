package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.Pat
import io.tolgee.model.UserAccount
import io.tolgee.model.UserPreferences
import io.tolgee.model.notifications.NotificationPreferences
import org.springframework.core.io.ClassPathResource

class UserAccountBuilder(
  val testDataBuilder: TestDataBuilder,
) : BaseEntityDataBuilder<UserAccount, UserAccountBuilder>() {
  var rawPassword = "admin"
  override var self: UserAccount = UserAccount()
  lateinit var defaultOrganizationBuilder: OrganizationBuilder

  class DATA {
    var avatarFile: ClassPathResource? = null
    var userPreferences: UserPreferencesBuilder? = null
    var pats: MutableList<PatBuilder> = mutableListOf()
    var notificationPreferences: MutableList<NotificationPreferencesBuilder> = mutableListOf()
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

  fun addPat(ft: FT<Pat>) = addOperation(data.pats, ft)

  fun addNotificationPreferences(ft: FT<NotificationPreferences>) = addOperation(data.notificationPreferences, ft)
}
