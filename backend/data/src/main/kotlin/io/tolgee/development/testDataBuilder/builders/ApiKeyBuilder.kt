package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.ApiKey

class ApiKeyBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<ApiKey, ApiKeyBuilder> {
  override var self: ApiKey =
    ApiKey(
      "test_api_key",
      mutableSetOf(),
    ).apply {
      project = projectBuilder.self
      projectBuilder.onlyUser?.let {
        this.userAccount = it
      }
    }
}
