package io.tolgee.component

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Component
class FrontendUrlProvider(
  private val tolgeeProperties: TolgeeProperties
) {
  val url: String
    get() {
      if (!tolgeeProperties.frontEndUrl.isNullOrBlank()) {
        return tolgeeProperties.frontEndUrl!!
      }

      val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
      builder.replacePath("")
      builder.replaceQuery("")
      return builder.build().toUriString()
    }

  fun getBillingUrl(organizationSlug: String): String {
    return "${this.url}/organizations/$organizationSlug/billing"
  }

  fun getBillingSelfHostedSubscriptionsUrl(organizationSlug: String): String {
    return "${this.url}/organizations/$organizationSlug/billing/self-hosted-ee"
  }
}
