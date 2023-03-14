package io.tolgee.ee.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.ee.EeProperties
import io.tolgee.ee.api.v2.hateoas.PrepareSetEeLicenceKeyModel
import io.tolgee.ee.api.v2.hateoas.SelfHostedEeSubscriptionModel
import io.tolgee.ee.data.GetMySubscriptionDto
import io.tolgee.ee.data.ReportErrorDto
import io.tolgee.ee.data.ReportUsageDto
import io.tolgee.ee.data.SetLicenseKeyLicensingDto
import io.tolgee.ee.data.SubscriptionStatus
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.security.UserAccountService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class EeSubscriptionService(
  private val eeSubscriptionRepository: EeSubscriptionRepository,
  private val restTemplate: RestTemplate,
  private val eeProperties: EeProperties,
  private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider
) {
  companion object {
    const val setPath: String = "/v2/public/licensing/set-key"
    const val prepareSetKeyPath: String = "/v2/public/licensing/prepare-set-key"
    const val subscriptionInfoPath: String = "/v2/public/licensing/subscription"
    const val reportUsagePath: String = "/v2/public/licensing/report-usage"
    const val reportErrorPath: String = "/v2/public/licensing/report-error"
  }

  fun getSubscription(): EeSubscription? {
    return eeSubscriptionRepository.findById(1).orElse(null)
  }

  fun setLicenceKey(licenseKey: String): EeSubscription {
    val seats = userAccountService.countAll()
    val response = try {
      postRequest<SelfHostedEeSubscriptionModel>(
        setPath,
        SetLicenseKeyLicensingDto(licenseKey, seats)
      )
    } catch (e: HttpClientErrorException.NotFound) {
      throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
    }

    val responseBody = response.body
    if (responseBody != null) {
      val entity = EeSubscription().apply {
        this.licenseKey = licenseKey
        this.name = responseBody.plan.name
        this.currentPeriodEnd = responseBody.currentPeriodEnd?.let { Date(it) }
        this.enabledFeatures = responseBody.plan.enabledFeatures
        this.lastValidCheck = currentDateProvider.date
      }
      return eeSubscriptionRepository.save(entity)
    }

    throw IllegalStateException("Licence not obtained.")
  }

  fun prepareSetLicenceKey(licenseKey: String): PrepareSetEeLicenceKeyModel {
    val seats = userAccountService.countAll()

    val response = try {
      postRequest<PrepareSetEeLicenceKeyModel>(
        prepareSetKeyPath,
        SetLicenseKeyLicensingDto(licenseKey, seats),
      )
    } catch (e: HttpClientErrorException.NotFound) {
      throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
    }

    val responseBody = response.body
    if (responseBody != null) {
      return responseBody
    }

    throw IllegalStateException("Licence not obtained")
  }

  private inline fun <reified T> postRequest(url: String, body: Any): ResponseEntity<T> {
    val bodyJson = jacksonObjectMapper().writeValueAsString(body)
    val headers = HttpHeaders().apply {
      contentType = MediaType.APPLICATION_JSON
    }
    return restTemplate.exchange(
      "${eeProperties.licenseServer}$url",
      HttpMethod.POST,
      HttpEntity(bodyJson, headers),
      T::class.java
    )
  }

  @Scheduled(fixedDelay = 1000 * 60 * 5)
  @Transactional
  fun checkSubscription() {
    val subscription = getSubscription()
    if (subscription != null) {
      val responseBody = try {
        getRemoteSubscriptionInfo(subscription)
      } catch (e: Exception) {
        reportError(e.stackTraceToString())
        null
      }
      updateLocalSubscription(responseBody, subscription)
      handleConstantlyFailingRemoteCheck(subscription)
    }
  }

  fun refreshSubscription() {
    val subscription = getSubscription()
    if (subscription != null) {
      val responseBody = getRemoteSubscriptionInfo(subscription)
      updateLocalSubscription(responseBody, subscription)
      handleConstantlyFailingRemoteCheck(subscription)
    }
  }

  private fun updateLocalSubscription(
    responseBody: SelfHostedEeSubscriptionModel?,
    subscription: EeSubscription
  ) {
    if (responseBody != null) {
      subscription.currentPeriodEnd = responseBody.currentPeriodEnd?.let { Date(it) }
      subscription.enabledFeatures = responseBody.plan.enabledFeatures
      subscription.status = responseBody.status
      subscription.lastValidCheck = currentDateProvider.date
      eeSubscriptionRepository.save(subscription)
    }
  }

  private fun handleConstantlyFailingRemoteCheck(subscription: EeSubscription) {
    subscription.lastValidCheck?.let {
      val isConstantlyFailing = currentDateProvider.date.time - it.time > 1000 * 60 * 60 * 24 * 2
      if (isConstantlyFailing) {
        subscription.status = SubscriptionStatus.ERROR
        eeSubscriptionRepository.save(subscription)
      }
    }
  }

  private fun getRemoteSubscriptionInfo(subscription: EeSubscription): SelfHostedEeSubscriptionModel? {
    val response = try {
      postRequest<SelfHostedEeSubscriptionModel>(
        subscriptionInfoPath,
        GetMySubscriptionDto(subscription.licenseKey),
      )
    } catch (e: HttpClientErrorException.NotFound) {
      subscription.status = SubscriptionStatus.CANCELED
      null
    }
    return response?.body
  }

  fun reportError(error: String) {
    try {
      getSubscription()?.let {
        postRequest<Any>(reportErrorPath, ReportErrorDto(error, it.licenseKey))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun reportUsage() {
    val subscription = getSubscription()
    if (subscription != null) {
      val seats = userAccountService.countAllEnabled()
      reportUsage(subscription, seats)
    }
  }

  private fun reportUsage(subscription: EeSubscription, seats: Long) {
    postRequest<Any>(
      reportUsagePath,
      ReportUsageDto(subscription.licenseKey, seats)
    )
  }

  @Transactional
  fun releaseSubscription() {
    val subscription = getSubscription()
    if (subscription != null) {
      reportUsage(subscription, 0)
      eeSubscriptionRepository.deleteAll()
    }
  }
}
