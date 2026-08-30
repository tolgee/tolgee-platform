package io.tolgee.hateoas.apps

import io.tolgee.service.apps.AppInstallService
import org.springframework.stereotype.Component

@Component
class AppRegisteredModelAssembler {
  fun toModel(result: AppInstallService.RegisterAppResult): AppRegisteredModel {
    val credentials = result.appCredentials
    return AppRegisteredModel(
      id = result.appEntityId,
      appId = result.app.appId,
      name = result.app.name,
      clientId = credentials?.clientId,
      clientSecret = credentials?.clientSecret,
      webhookSecret = credentials?.webhookSecret,
      installId = result.install?.id,
      delivery = AppDeliveryOutcomeModel.of(result.delivery),
    )
  }
}
