package io.tolgee.ee.service.eeSubscription.cloudClient

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.component.HttpClient
import io.tolgee.constants.Message
import io.tolgee.ee.EeProperties
import io.tolgee.ee.data.GetMySubscriptionDto
import io.tolgee.ee.data.GetMySubscriptionUsageRequest
import io.tolgee.ee.data.PrepareSetLicenseKeyDto
import io.tolgee.ee.data.ReleaseKeyDto
import io.tolgee.ee.data.ReportErrorDto
import io.tolgee.ee.data.ReportUsageDto
import io.tolgee.ee.data.SetLicenseKeyLicensingDto
import io.tolgee.ee.model.EeSubscription
import io.tolgee.exceptions.BadRequestException
import io.tolgee.hateoas.ee.PrepareSetEeLicenceKeyModel
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import io.tolgee.hateoas.ee.uasge.current.CurrentUsageModel
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException

@Component
class TolgeeCloudLicencingClient(
  private val httpClient: HttpClient,
  private val eeProperties: EeProperties,
) {
  companion object {
    const val SET_PATH: String = "/v2/public/licensing/set-key"
    const val PREPARE_SET_KEY_PATH: String = "/v2/public/licensing/prepare-set-key"
    const val SUBSCRIPTION_INFO_PATH: String = "/v2/public/licensing/subscription"
    const val SUBSCRIPTION_USAGE_PATH: String = "/v2/public/licensing/current-subscription-usage"
    const val REPORT_USAGE_PATH: String = "/v2/public/licensing/report-usage"
    const val RELEASE_KEY_PATH: String = "/v2/public/licensing/release-key"
    const val REPORT_ERROR_PATH: String = "/v2/public/licensing/report-error"
  }

  internal fun getRemoteSubscriptionInfo(
    licenseKey: String,
    instanceId: String,
  ): SelfHostedEeSubscriptionModel? {
    val responseBody =
      postRequest<SelfHostedEeSubscriptionModel>(
        SUBSCRIPTION_INFO_PATH,
        GetMySubscriptionDto(licenseKey, instanceId),
      )
    return responseBody
  }

  fun reportErrorRemote(
    error: String,
    licenseKey: String,
  ) = postRequest<Any>(REPORT_ERROR_PATH, ReportErrorDto(error, licenseKey))

  fun reportUsageRemote(
    subscription: EeSubscriptionDto,
    keys: Long?,
    seats: Long?,
  ) {
    postRequest<Unit>(
      REPORT_USAGE_PATH,
      ReportUsageDto(licenseKey = subscription.licenseKey, keys = keys, seats = seats),
    )
  }

  internal fun releaseKeyRemote(subscription: EeSubscription) {
    postRequest<Unit>(
      RELEASE_KEY_PATH,
      ReleaseKeyDto(subscription.licenseKey),
    )
  }

  fun setLicenseKeyRemote(dto: SetLicenseKeyLicensingDto): SelfHostedEeSubscriptionModel {
    return try {
      postRequest<SelfHostedEeSubscriptionModel>(
        SET_PATH,
        dto,
      )
    } catch (e: HttpClientErrorException.NotFound) {
      throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
    }
  }

  fun prepareSetLicenseKeyRemote(dto: PrepareSetLicenseKeyDto): PrepareSetEeLicenceKeyModel {
    return try {
      postRequest<PrepareSetEeLicenceKeyModel>(
        PREPARE_SET_KEY_PATH,
        dto,
      )
    } catch (e: HttpClientErrorException.NotFound) {
      throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
    }
  }

  private inline fun <reified T> postRequest(
    url: String,
    body: Any,
  ): T {
    return httpClient.requestForJson("${eeProperties.licenseServer}$url", body, HttpMethod.POST, T::class.java)
  }

  fun getUsageRemote(licenseKey: String): CurrentUsageModel {
    return postRequest<CurrentUsageModel>(
      SUBSCRIPTION_USAGE_PATH,
      GetMySubscriptionUsageRequest(licenseKey),
    )
  }
}
