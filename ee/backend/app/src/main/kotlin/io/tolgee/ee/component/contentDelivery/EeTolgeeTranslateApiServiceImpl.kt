package io.tolgee.ee.component.contentDelivery

import io.tolgee.component.HttpClient
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.tolgee.EeTolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateParams
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.ee.service.EeSubscriptionServiceImpl
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.OutOfCreditsException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException.BadRequest

@Component
class EeTolgeeTranslateApiServiceImpl(
  private val tolgeeProperties: TolgeeProperties,
  private val httpClient: HttpClient,
  private val subscriptionService: EeSubscriptionServiceImpl,
  private val eeSubscriptionServiceImpl: EeSubscriptionServiceImpl,
) : TolgeeTranslateApiService, EeTolgeeTranslateApiService {
  companion object {
    const val API_PATH = "v2/public/translator/translate"
  }

  override fun translate(params: TolgeeTranslateParams): MtValueProvider.MtResult {
    val url = tolgeeProperties.machineTranslation.tolgee.url + "/" + API_PATH
    val licenseKey =
      subscriptionService.findSubscriptionDto()?.licenseKey ?: throw IllegalStateException("Not Subscribed")

    try {
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
    } catch (e: BadRequest) {
      if (e.message?.contains(Message.CREDIT_SPENDING_LIMIT_EXCEEDED.code) == true) {
        throw OutOfCreditsException(OutOfCreditsException.Reason.SPENDING_LIMIT_EXCEEDED, e)
      }
      if (e.message?.contains(Message.SUBSCRIPTION_NOT_ACTIVE.code) == true) {
        eeSubscriptionServiceImpl.checkSubscription()
        throw BadRequestException(Message.SUBSCRIPTION_NOT_ACTIVE)
      }
      throw e
    }
  }

  class EmptyBodyException : Exception("Empty body")
}
