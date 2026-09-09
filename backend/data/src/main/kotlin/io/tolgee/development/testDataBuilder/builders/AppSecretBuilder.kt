package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.apps.AppSecret
import java.util.UUID

class AppSecretBuilder(
  val appBuilder: AppBuilder,
) : EntityDataBuilder<AppSecret, AppSecretBuilder> {
  override var self: AppSecret =
    AppSecret().apply {
      app = appBuilder.self
      secretHash = UUID.randomUUID().toString().replace("-", "")
      name = "tgpubs_ab…yz"
    }
}
