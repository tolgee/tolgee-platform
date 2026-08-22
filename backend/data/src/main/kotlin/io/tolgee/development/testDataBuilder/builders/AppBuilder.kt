package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppAvailability
import io.tolgee.model.apps.AppSecret
import java.util.UUID

class AppBuilder(
  val organizationBuilder: OrganizationBuilder,
) : BaseEntityDataBuilder<App, AppBuilder>() {
  class DATA {
    val secrets = mutableListOf<AppSecretBuilder>()
    val availabilities = mutableListOf<AppAvailabilityBuilder>()
  }

  val data = DATA()

  override var self: App =
    App().apply {
      organization = organizationBuilder.self
      appId = "test-app"
      manifestUrl = "https://example.com/manifest.json"
      name = "Test App"
      version = "0.1.0"
      baseUrl = "https://app.example.com"
      manifestJson = DEFAULT_MANIFEST_JSON
      manifestScopes = ""
      clientId = "tgpub_" + UUID.randomUUID().toString().replace("-", "")
      webhookSecret = UUID.randomUUID().toString().replace("-", "")
    }

  fun addSecret(ft: FT<AppSecret>) = addOperation(data.secrets, ft)

  fun addAvailability(ft: FT<AppAvailability>) = addOperation(data.availabilities, ft)

  /** The sentinel row (null organization): makes the app available to every organization. */
  fun addAvailableToAll(): AppAvailabilityBuilder = addAvailability { organization = null }

  companion object {
    private const val DEFAULT_MANIFEST_JSON =
      "{\"id\":\"test-app\",\"name\":\"Test App\",\"version\":\"0.1.0\"," +
        "\"baseUrl\":\"https://app.example.com\"," +
        "\"modules\":{\"project-dashboard-page\":" +
        "[{\"key\":\"home\",\"title\":\"Home\",\"icon\":\"🏠\",\"entry\":\"/\"}]}}"
  }
}
