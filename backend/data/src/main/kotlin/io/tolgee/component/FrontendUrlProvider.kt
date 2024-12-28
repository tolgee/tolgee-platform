package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class FrontendUrlProvider(
  private val tolgeeProperties: TolgeeProperties,
) {
  val url: String
    get() {
      val frontEndUrlFromProperties = tolgeeProperties.frontEndUrl
      if (!frontEndUrlFromProperties.isNullOrBlank()) {
        return frontEndUrlFromProperties
      }

      return getFromServerRequest()
    }

  private fun getFromServerRequest(): String {
    try {
      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      builder.replacePath("")
      builder.replaceQuery("")
      return builder.build().toUriString()
    } catch (e: IllegalStateException) {
      if (e.message?.contains("No current ServletRequestAttributes") == true) {
        throw IllegalStateException(
          "Trying to find frontend url, but there is no current request. " +
            "You will have to specify frontend url in application properties."
        )
      }
      throw e
    }
  }

  fun getSubscriptionsUrl(organizationSlug: String): String {
    return "${this.url}/organizations/$organizationSlug/subscriptions"
  }

  fun getInvoicesUrl(organizationSlug: String): String {
    return "${this.url}/organizations/$organizationSlug/invoices"
  }

  fun getSelfHostedSubscriptionsUrl(organizationSlug: String): String {
    return "${this.url}/organizations/$organizationSlug/subscriptions/self-hosted-ee"
  }
}
