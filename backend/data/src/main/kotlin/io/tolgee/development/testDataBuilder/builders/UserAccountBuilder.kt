package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.UserAccount
import org.springframework.core.io.ClassPathResource

class UserAccountBuilder(
  val testDataBuilder: TestDataBuilder
) : EntityDataBuilder<UserAccount, UserAccountBuilder> {
  var rawPassword = "admin"
  override var self: UserAccount = UserAccount()

  class DATA {
    var avatarFile: ClassPathResource? = null
  }

  var data = DATA()

  fun addMtCreditBucket(ft: FT<MtCreditBucket>): MtCreditBucketBuilder {
    val builder = MtCreditBucketBuilder()
    testDataBuilder.data.mtCreditBuckets.add(builder)
    builder.self.userAccount = this.self
    ft(builder.self)
    return builder
  }

  fun setAvatar(filePath: String) {
    data.avatarFile = ClassPathResource(filePath, this.javaClass.classLoader)
  }
}
