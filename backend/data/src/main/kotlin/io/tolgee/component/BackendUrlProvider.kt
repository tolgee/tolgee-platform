package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.nullIfBlank
import org.springframework.stereotype.Component

@Component
class BackendUrlProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  /**
   * The configured URL of the API, or null when `tolgee.back-end-url` is unset.
   *
   * There is deliberately no request-derived fallback of the kind [FrontendUrlProvider.url] has: `X-Forwarded-*` is
   * untrusted here (see `server.forward-headers-strategy` in application.yaml), so behind a TLS-terminating proxy
   * the container's view of the request is the internal URL, and anything built from it would be unreachable.
   */
  val stableUrl: String?
    get() = tolgeeProperties.backEndUrl.nullIfBlank
}
