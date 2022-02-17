package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.UserAccount

class UserAccountBuilder(
  val testDataBuilder: TestDataBuilder
) : EntityDataBuilder<UserAccount, UserAccountBuilder> {
  var rawPassword = "admin"
  override var self: UserAccount = UserAccount()

  fun addMtCreditBucket(ft: FT<MtCreditBucket>): MtCreditBucketBuilder {
    val builder = MtCreditBucketBuilder()
    testDataBuilder.data.mtCreditBuckets.add(builder)
    builder.self.userAccount = this.self
    ft(builder.self)
    return builder
  }
}
