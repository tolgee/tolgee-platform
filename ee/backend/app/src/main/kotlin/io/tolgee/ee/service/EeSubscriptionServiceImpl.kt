package io.tolgee.ee.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.api.EeSubscriptionDto
import io.tolgee.api.EeSubscriptionProvider
import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.HttpClient
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.ee.EeProperties
import io.tolgee.ee.data.*
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.limits.PlanKeysLimitExceeded
import io.tolgee.exceptions.limits.PlanSeatLimitExceeded
import io.tolgee.hateoas.ee.PrepareSetEeLicenceKeyModel
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import io.tolgee.service.InstanceIdService
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import java.util.*

@Service
class EeSubscriptionServiceImpl(
  private val eeSubscriptionRepository: EeSubscriptionRepository,
  private val eeProperties: EeProperties,
  private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
  private val httpClient: HttpClient,
  private val instanceIdService: InstanceIdService,
  private val platformTransactionManager: PlatformTransactionManager,
  @Suppress("SelfReferenceConstructorParameter") @Lazy
  private val self: EeSubscriptionServiceImpl,
  private val billingConfProvider: PublicBillingConfProvider,
  private val keyService: KeyService,
) : EeSubscriptionProvider, Logging {
  companion object {
    const val SET_PATH: String = "/v2/public/licensing/set-key"
    const val PREPARE_SET_KEY_PATH: String = "/v2/public/licensing/prepare-set-key"
    const val SUBSCRIPTION_INFO_PATH: String = "/v2/public/licensing/subscription"
    const val REPORT_USAGE_PATH: String = "/v2/public/licensing/report-usage"
    const val RELEASE_KEY_PATH: String = "/v2/public/licensing/release-key"
    const val REPORT_ERROR_PATH: String = "/v2/public/licensing/report-error"
  }

  var bypassSeatCountCheck = false

  @Cacheable(Caches.EE_SUBSCRIPTION, key = "1")
  override fun findSubscriptionDto(): EeSubscriptionDto? {
    return this.findSubscriptionEntity()?.toDto()
  }

  fun findSubscriptionEntity(): EeSubscription? {
    return eeSubscriptionRepository.findById(1).orElse(null)
  }

  fun isSubscribed(): Boolean {
    return self.findSubscriptionDto() != null
  }

  @CacheEvict(Caches.EE_SUBSCRIPTION, key = "1")
  fun setLicenceKey(licenseKey: String): EeSubscription {
    val seats = userAccountService.countAllEnabled()
    val keys = keyService.countAllOnInstance()
    this.findSubscriptionEntity()?.let {
      throw BadRequestException(Message.THIS_INSTANCE_IS_ALREADY_LICENSED)
    }

    val entity =
      EeSubscription().apply {
        this.licenseKey = licenseKey
        this.lastValidCheck = currentDateProvider.date
      }

    val responseBody =
      catchingSeatsSpendingLimit {
        try {
          postRequest<SelfHostedEeSubscriptionModel>(
            SET_PATH,
            SetLicenseKeyLicensingDto(
              licenseKey = licenseKey,
              seats = seats,
              keys = keys,
              instanceId = instanceIdService.getInstanceId(),
            ),
          )
        } catch (e: HttpClientErrorException.NotFound) {
          throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
        }
      }

    if (responseBody != null) {
      entity.name = responseBody.plan.name
      entity.currentPeriodEnd = responseBody.currentPeriodEnd?.let { Date(it) }
      entity.enabledFeatures = responseBody.plan.enabledFeatures
      entity.nonCommercial = responseBody.plan.nonCommercial
      entity.includedKeys = responseBody.plan.includedUsage.keys
      entity.includedSeats = responseBody.plan.includedUsage.seats
      entity.isPayAsYouGo = responseBody.plan.isPayAsYouGo
      return self.save(entity)
    }

    throw IllegalStateException("Licence not obtained.")
  }

  fun prepareSetLicenceKey(licenseKey: String): PrepareSetEeLicenceKeyModel {
    val seats = userAccountService.countAllEnabled()
    val responseBody =
      catchingSeatsSpendingLimit {
        try {
          postRequest<PrepareSetEeLicenceKeyModel>(
            PREPARE_SET_KEY_PATH,
            PrepareSetLicenseKeyDto(licenseKey, seats),
          )
        } catch (e: HttpClientErrorException.NotFound) {
          throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
        }
      }

    if (responseBody != null) {
      return responseBody
    }

    throw IllegalStateException("Licence not obtained")
  }

  fun <T> catchingSeatsSpendingLimit(fn: () -> T): T {
    return try {
      fn()
    } catch (e: HttpClientErrorException.BadRequest) {
      val body = e.parseBody()
      if (body.code == Message.SEATS_SPENDING_LIMIT_EXCEEDED.code) {
        throw BadRequestException(body.code, body.params)
      }
      throw e
    }
  }

  private inline fun <reified T> postRequest(
    url: String,
    body: Any,
  ): T? {
    return httpClient.requestForJson("${eeProperties.licenseServer}$url", body, HttpMethod.POST, T::class.java)
  }

  @Scheduled(fixedDelayString = """${'$'}{tolgee.ee.check-period-ms:300000}""")
  @Transactional
  fun checkSubscription() {
    refreshSubscription()
  }

  @CacheEvict(Caches.EE_SUBSCRIPTION, key = "1")
  fun refreshSubscription() {
    val subscription = this.findSubscriptionEntity()
    if (subscription != null) {
      val responseBody =
        try {
          getRemoteSubscriptionInfo(subscription)
        } catch (e: HttpClientErrorException.BadRequest) {
          val error = e.parseBody()
          if (error.code == Message.LICENSE_KEY_USED_BY_ANOTHER_INSTANCE.code) {
            setSubscriptionKeyUsedByOtherInstance(subscription)
          }
          null
        } catch (e: Exception) {
          reportError(e.stackTraceToString())
          null
        }
      updateLocalSubscription(responseBody, subscription)
      handleConstantlyFailingRemoteCheck(subscription)
    }
  }

  private fun setSubscriptionKeyUsedByOtherInstance(subscription: EeSubscription) {
    subscription.status = SubscriptionStatus.KEY_USED_BY_ANOTHER_INSTANCE
    self.save(subscription)
  }

  fun HttpClientErrorException.parseBody(): ErrorResponseBody {
    return jacksonObjectMapper().readValue(this.responseBodyAsString, ErrorResponseBody::class.java)
  }

  private fun updateLocalSubscription(
    responseBody: SelfHostedEeSubscriptionModel?,
    subscription: EeSubscription,
  ) {
    if (responseBody != null) {
      subscription.currentPeriodEnd = responseBody.currentPeriodEnd?.let { Date(it) }
      subscription.enabledFeatures = responseBody.plan.enabledFeatures
      subscription.status = responseBody.status
      subscription.lastValidCheck = currentDateProvider.date
      subscription.nonCommercial = responseBody.plan.nonCommercial
      self.save(subscription)
    }
  }

  private fun handleConstantlyFailingRemoteCheck(subscription: EeSubscription) {
    subscription.lastValidCheck?.let {
      val isConstantlyFailing = currentDateProvider.date.time - it.time > 1000 * 60 * 60 * 24 * 2
      if (isConstantlyFailing) {
        subscription.status = SubscriptionStatus.ERROR
        self.save(subscription)
      }
    }
  }

  private fun getRemoteSubscriptionInfo(subscription: EeSubscription): SelfHostedEeSubscriptionModel? {
    val responseBody =
      try {
        postRequest<SelfHostedEeSubscriptionModel>(
          SUBSCRIPTION_INFO_PATH,
          GetMySubscriptionDto(subscription.licenseKey, instanceIdService.getInstanceId()),
        )
      } catch (e: HttpClientErrorException.NotFound) {
        subscription.status = SubscriptionStatus.CANCELED
        null
      }
    return responseBody
  }

  fun reportError(error: String) {
    try {
      findSubscriptionEntity()?.let {
        postRequest<Any>(REPORT_ERROR_PATH, ReportErrorDto(error, it.licenseKey))
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun checkSeatCount(
    subscription: EeSubscriptionDto?,
    seats: Long,
  ) {
    if (bypassSeatCountCheck || isCloud || subscription?.isPayAsYouGo == true) {
      return
    }

    val limit = subscription?.includedSeats ?: 10

    if (seats > limit) {
      when (subscription) {
        null -> throw BadRequestException(Message.FREE_SELF_HOSTED_SEAT_LIMIT_EXCEEDED)
        else -> throw PlanSeatLimitExceeded(seats, limit)
      }
    }
  }

  @Transactional
  fun checkKeyCount(
    keys: Long,
  ) {
    val subscription = findSubscriptionDto()

    if (
      isCloud ||
      subscription == null ||
      subscription.isPayAsYouGo ||
      subscription.includedKeys < 0
    ) {
      return
    }

    if (keys > subscription.includedKeys) {
      throw PlanKeysLimitExceeded(keys, subscription.includedKeys)
    }
  }

  fun reportUsage(
    subscription: EeSubscriptionDto?,
    keys: Long? = null,
    seats: Long? = null,
  ) {
    if (subscription != null) {
      catchingSeatsSpendingLimit {
        catchingLicenseNotFound {
          reportUsageRemote(subscription = subscription, keys = keys, seats = seats)
        }
      }
    }
  }

  fun <T> catchingLicenseNotFound(fn: () -> T): T {
    try {
      return fn()
    } catch (e: HttpClientErrorException.NotFound) {
      val licenceKeyNotFound = e.message?.contains(Message.LICENSE_KEY_NOT_FOUND.code) == true
      if (!licenceKeyNotFound) {
        throw e
      }
      executeInNewTransaction(platformTransactionManager) {
        val entity = findSubscriptionEntity() ?: throw NoActiveSubscriptionException()
        entity.status = SubscriptionStatus.ERROR
        self.save(entity)
        throw e
      }
      throw e
    }
  }

  @CacheEvict(Caches.EE_SUBSCRIPTION, key = "1")
  fun save(subscription: EeSubscription): EeSubscription {
    return eeSubscriptionRepository.save(subscription)
  }

  private fun reportUsageRemote(
    subscription: EeSubscriptionDto,
    keys: Long?,
    seats: Long?,
  ) {
    postRequest<Any>(
      REPORT_USAGE_PATH,
      ReportUsageDto(licenseKey = subscription.licenseKey, keys = keys, seats = seats),
    )
  }

  private fun releaseKeyRemote(subscription: EeSubscription) {
    postRequest<Any>(
      RELEASE_KEY_PATH,
      ReleaseKeyDto(subscription.licenseKey),
    )
  }

  @Transactional
  @CacheEvict(Caches.EE_SUBSCRIPTION, key = "1")
  fun releaseSubscription() {
    val subscription = findSubscriptionEntity()
    if (subscription != null) {
      try {
        releaseKeyRemote(subscription)
      } catch (e: HttpClientErrorException.NotFound) {
        val licenceKeyNotFound = e.message?.contains(Message.LICENSE_KEY_NOT_FOUND.code) == true
        if (!licenceKeyNotFound) {
          throw e
        }
      }

      eeSubscriptionRepository.deleteAll()
    }
  }

  private val isCloud: Boolean
    get() = billingConfProvider.invoke().enabled
}
