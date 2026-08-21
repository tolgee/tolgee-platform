package io.tolgee.hateoas.organization.apps

import io.tolgee.service.apps.AppManifestFetcher
import org.springframework.stereotype.Component

@Component
class AppManifestPreviewModelAssembler {
  fun toModel(fetched: AppManifestFetcher.FetchResult): AppManifestPreviewModel {
    return AppManifestPreviewModel(
      appId = fetched.manifest.id,
      name = fetched.manifest.name,
      version = fetched.manifest.version,
      baseUrl = fetched.manifest.baseUrl,
      icon = fetched.icon,
      modules = fetched.manifest.modules,
      requestedScopes = fetched.scopes.map { it.value },
    )
  }
}
