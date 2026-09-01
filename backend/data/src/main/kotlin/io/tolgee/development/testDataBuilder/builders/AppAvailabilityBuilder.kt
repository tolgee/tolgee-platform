package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.apps.AppAvailability

class AppAvailabilityBuilder(
  val appBuilder: AppBuilder,
) : EntityDataBuilder<AppAvailability, AppAvailabilityBuilder> {
  override var self: AppAvailability =
    AppAvailability().apply {
      app = appBuilder.self
    }
}
