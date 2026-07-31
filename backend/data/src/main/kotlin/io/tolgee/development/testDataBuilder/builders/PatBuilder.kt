package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Pat

class PatBuilder(
  val userAccountBuilder: UserAccountBuilder,
) : EntityDataBuilder<Pat, PatBuilder> {
  // tokenHash is left blank on purpose: PatService.save (used by TestDataService) regenerates a
  // unique hash when blank, which the unique pat_token_hash_unique constraint requires.
  override var self: Pat =
    Pat(description = "Test PAT")
      .apply { userAccount = userAccountBuilder.self }
}
