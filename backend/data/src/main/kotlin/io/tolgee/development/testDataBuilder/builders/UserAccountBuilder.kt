package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.UserAccount
import org.springframework.core.io.ClassPathResource

class UserAccountBuilder(
  val testDataBuilder: TestDataBuilder
) : EntityDataBuilder<UserAccount, UserAccountBuilder> {
  var rawPassword = "admin"
  override var self: UserAccount = UserAccount()
  lateinit var defaultOrganizationBuilder: OrganizationBuilder

  class DATA {
    var avatarFile: ClassPathResource? = null
  }

  var data = DATA()

  fun setAvatar(filePath: String) {
    data.avatarFile = ClassPathResource(filePath, this.javaClass.classLoader)
  }
}
