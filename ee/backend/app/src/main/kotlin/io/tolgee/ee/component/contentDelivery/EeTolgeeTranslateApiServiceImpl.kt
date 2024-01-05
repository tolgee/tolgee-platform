package io.tolgee.ee.component.contentDelivery

import io.tolgee.component.HttpClient
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.tolgee.EeTolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateParams
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.service.EeSubscriptionService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component

@Component
class EeTolgeeTranslateApiServiceImpl(
  private val tolgeeProperties: TolgeeProperties,
  private val httpClient: HttpClient,
  private val subscriptionService: EeSubscriptionService,
) : TolgeeTranslateApiService, EeTolgeeTranslateApiService {
  companion object {
    const val API_PATH = "v2/public/translator/translate"
  }

  override fun translate(params: TolgeeTranslateParams): MtValueProvider.MtResult {
    val url = tolgeeProperties.machineTranslation.tolgee.url + "/" + API_PATH
    val licenseKey =
      subscriptionService.findSubscriptionDto()?.licenseKey ?: throw IllegalStateException("Not Subscribed")

    return subscriptionService.catchingLicenseNotFound {
      httpClient.requestForJson(
        url = url,
        body = params,
        method = HttpMethod.POST,
        result = MtValueProvider.MtResult::class.java,
        headers =
          HttpHeaders().apply {
            this.add("License-Key", licenseKey)
          },
      ) ?: throw EmptyBodyException()
    }
  }

  class EmptyBodyException : Exception("Empty body")
}
