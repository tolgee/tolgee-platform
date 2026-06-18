package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component

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
