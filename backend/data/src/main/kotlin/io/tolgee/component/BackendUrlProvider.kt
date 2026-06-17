package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component

/**
 * Resolves the public origin of the backend (where the `/v2` API routes are served), preferring
 * `tolgee.back-end-url` and falling back to the current servlet-request origin.
 */
@Component
class BackendUrlProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  val url: String
    get() {
      val backEndUrl = tolgeeProperties.backEndUrl
      if (!backEndUrl.isNullOrBlank()) {
        return backEndUrl
      }
      return currentRequestOriginOrNull()
        ?: throw IllegalStateException(
          "Trying to find backend url, but there is no current request. " +
            "You will have to specify tolgee.back-end-url in application properties.",
        )
    }
}
