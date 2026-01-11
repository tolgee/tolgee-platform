package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Pat

class PatBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<Pat, PatBuilder> {
  override var self: Pat = Pat().apply { userAccount = userAccountBuilder.self }
}
