package io.tolgee.ee.service.eeSubscription

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.api.EeSubscriptionDto
import io.tolgee.api.EeSubscriptionProvider
import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.ee.component.limitsAndReporting.SelfHostedLimitsProvider
import io.tolgee.ee.component.limitsAndReporting.generic.SeatsLimitChecker
import io.tolgee.ee.data.*
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.NoActiveSubscriptionException
import io.tolgee.ee.service.eeSubscription.cloudClient.TolgeeCloudLicencingClient
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
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
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import java.util.*

@Service
class EeSubscriptionServiceImpl(
  private val eeSubscriptionRepository: EeSubscriptionRepository,
  private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
  private val instanceIdService: InstanceIdService,
  @Suppress("SelfReferenceConstructorParameter") @Lazy
  private val self: EeSubscriptionServiceImpl,
  private val keyService: KeyService,
  private val selfHostedLimitsProvider: SelfHostedLimitsProvider,
  private val client: TolgeeCloudLicencingClient,
  private val catchingService: EeSubscriptionErrorCatchingService
) : EeSubscriptionProvider, Logging {
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


    val response = catchingService.catchingSpendingLimits {
      client.setLicenseKeyRemote(
        SetLicenseKeyLicensingDto(
          licenseKey = licenseKey,
          seats = seats,
          keys = keys,
          instanceId = instanceIdService.getInstanceId(),
        )
      )
    }

    SubscriptionFromModelAssigner(entity, response, currentDateProvider.date).assign()
    return self.save(entity)
  }

  fun prepareSetLicenceKey(licenseKey: String): PrepareSetEeLicenceKeyModel {
    val seats = userAccountService.countAllEnabled()
    val responseBody = catchingService.catchingSpendingLimits {
      client.prepareSetLicenseKeyRemote(PrepareSetLicenseKeyDto(licenseKey, seats))
    }
      return responseBody
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
          catchingService.catchingLicenseNotFound {
            catchingService.catchingLicenseUsedByAnotherInstance {
              client.getRemoteSubscriptionInfo(subscription.licenseKey, instanceIdService.getInstanceId())
          }
          }
        } catch (e: Exception) {
          reportError(e.stackTraceToString())
          null
        }
      updateLocalSubscription(responseBody, subscription)
      handleConstantlyFailingRemoteCheck(subscription)
    }
  }

  private fun updateLocalSubscription(
    responseBody: SelfHostedEeSubscriptionModel?,
    subscription: EeSubscription,
  ) {
    if (responseBody != null) {
      SubscriptionFromModelAssigner(subscription, responseBody, currentDateProvider.date).assign()
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

  fun checkSeatCount(
    subscription: EeSubscriptionDto?,
    seats: Long,
  ) {
    object : SeatsLimitChecker(
      required = seats,
      limits = selfHostedLimitsProvider.getLimits(),
    ) {
      override fun getIncludedUsageExceededException(): BadRequestException {
        self.findSubscriptionDto()
          ?: return BadRequestException(Message.FREE_SELF_HOSTED_SEAT_LIMIT_EXCEEDED)
        return super.getIncludedUsageExceededException()
      }
    }.check()
  }

  @CacheEvict(Caches.EE_SUBSCRIPTION, key = "1")
  fun save(subscription: EeSubscription): EeSubscription {
    return eeSubscriptionRepository.save(subscription)
  }

  @Transactional
  @CacheEvict(Caches.EE_SUBSCRIPTION, key = "1")
  fun releaseSubscription() {
    val subscription = findSubscriptionEntity()
    if (subscription != null) {
      try {
        client.releaseKeyRemote(subscription)
      } catch (e: HttpClientErrorException.NotFound) {
        val licenceKeyNotFound = e.message?.contains(Message.LICENSE_KEY_NOT_FOUND.code) == true
        if (!licenceKeyNotFound) {
          throw e
        }
      }

      eeSubscriptionRepository.deleteAll()
    }
  }

  fun reportUsage(
    subscription: EeSubscriptionDto?,
    keys: Long? = null,
    seats: Long? = null,
  ) {
    if (subscription != null) {
      catchingService.catchingSpendingLimits {
        catchingService.catchingLicenseNotFound {
          client.reportUsageRemote(subscription = subscription, keys = keys, seats = seats)
        }
      }
    }
  }

  fun reportError(error: String) {
    try {
      findSubscriptionEntity()?.let {
        client.reportErrorRemote(error, it.licenseKey)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
