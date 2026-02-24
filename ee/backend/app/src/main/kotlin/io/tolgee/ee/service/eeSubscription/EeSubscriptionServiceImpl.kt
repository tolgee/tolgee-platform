package io.tolgee.ee.service.eeSubscription

import io.tolgee.api.EeSubscriptionDto
import io.tolgee.api.EeSubscriptionProvider
import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.ee.EeProperties
import io.tolgee.ee.component.limitsAndReporting.SelfHostedLimitsProvider
import io.tolgee.ee.component.limitsAndReporting.generic.SeatsLimitChecker
import io.tolgee.ee.data.PrepareSetLicenseKeyDto
import io.tolgee.ee.data.SetLicenseKeyLicensingDto
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.eeSubscription.cloudClient.TolgeeCloudLicencingClient
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.limits.PlanLimitExceededSeatsException
import io.tolgee.hateoas.ee.PrepareSetEeLicenceKeyModel
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import io.tolgee.service.InstanceIdService
import io.tolgee.service.key.KeyService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import java.time.Duration

@Service
@Suppress("SelfReferenceConstructorParameter")
class EeSubscriptionServiceImpl(
  private val eeSubscriptionRepository: EeSubscriptionRepository,
  private val userAccountService: UserAccountService,
  private val currentDateProvider: CurrentDateProvider,
  private val instanceIdService: InstanceIdService,
  @Lazy
  private val self: EeSubscriptionServiceImpl,
  private val keyService: KeyService,
  private val selfHostedLimitsProvider: SelfHostedLimitsProvider,
  private val client: TolgeeCloudLicencingClient,
  private val catchingService: EeSubscriptionErrorCatchingService,
  private val schedulingManager: SchedulingManager,
  private val eeProperties: EeProperties,
) : EeSubscriptionProvider,
  Logging {
  var bypassSeatCountCheck = false

  @Cacheable(Caches.Companion.EE_SUBSCRIPTION, key = "1")
  override fun findSubscriptionDto(): EeSubscriptionDto? {
    logger.debug("EE subscription is being fetched from database.")
    return this.findSubscriptionEntity()?.toDto()
  }

  override fun getLicensingUrl(): String? {
    return eeProperties.licenseServer
  }

  fun findSubscriptionEntity(): EeSubscription? {
    return eeSubscriptionRepository.findById(1).orElse(null)
  }

  fun isSubscribed(): Boolean {
    return self.findSubscriptionDto() != null
  }

  @CacheEvict(Caches.Companion.EE_SUBSCRIPTION, key = "1")
  fun setLicenceKey(licenseKey: String): EeSubscription {
    logger.debug("Setting new licence key for local subscription: $licenseKey. Evicting cache.")
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

    val response =
      catchingService.catchingSpendingLimits {
        client.setLicenseKeyRemote(
          SetLicenseKeyLicensingDto(
            licenseKey = licenseKey,
            seats = seats,
            keys = keys,
            instanceId = instanceIdService.getInstanceId(),
          ),
        )
      }

    SubscriptionFromModelAssigner(entity, response, currentDateProvider.date).assign()
    return self.save(entity)
  }

  fun prepareSetLicenceKey(licenseKey: String): PrepareSetEeLicenceKeyModel {
    val seats = userAccountService.countAllEnabled()
    val responseBody =
      catchingService.catchingSpendingLimits {
        client.prepareSetLicenseKeyRemote(PrepareSetLicenseKeyDto(licenseKey, seats))
      }
    return responseBody
  }

  @EventListener(ApplicationReadyEvent::class)
  @Transactional
  fun scheduleSubscriptionChecking() {
    logger.debug("Scheduling ee subscription checking with period ${eeProperties.checkPeriodInMs} ms.")
    schedulingManager.scheduleWithFixedDelay({
      refreshSubscription()
    }, Duration.ofMillis(eeProperties.checkPeriodInMs))
  }

  @CacheEvict(Caches.Companion.EE_SUBSCRIPTION, key = "1")
  fun refreshSubscription() {
    logger.debug("Refreshing local ee subscription, evicting cache.")
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
      val isConstantlyFailing = currentDateProvider.date.time - it.time > Duration.ofDays(2).toMillis()
      if (isConstantlyFailing) {
        logger.error(
          "Remote check for local subscription has been failing for too long. " +
            "Setting status to ERROR.",
        )
        subscription.status = SubscriptionStatus.ERROR
        self.save(subscription)
      }
    }
  }

  fun checkSeatCount(seats: Long) {
    if (bypassSeatCountCheck) {
      return
    }

    val limits = selfHostedLimitsProvider.getLimits()
    SeatsLimitChecker(
      limits = limits,
      includedUsageExceededExceptionProvider = { req ->
        self
          .findSubscriptionDto()
          ?.let { PlanLimitExceededSeatsException(req, limit = limits.seats.limit) }
          ?: BadRequestException(Message.FREE_SELF_HOSTED_SEAT_LIMIT_EXCEEDED)
      },
    ).check(seats)
  }

  @CacheEvict(Caches.Companion.EE_SUBSCRIPTION, key = "1")
  fun save(subscription: EeSubscription): EeSubscription {
    logger.debug("Saving local ee subscription, evicting cache.")
    return eeSubscriptionRepository.save(subscription)
  }

  @Transactional
  @CacheEvict(Caches.Companion.EE_SUBSCRIPTION, key = "1")
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

  fun reportError(error: String) {
    try {
      findSubscriptionEntity()?.let {
        client.reportErrorRemote(error, it.licenseKey)
      }
    } catch (e: Exception) {
      logger.error("Cannot report error", e)
    }
  }

  /**
   * Deletes the license entry.
   * Only for testing
   */
  @CacheEvict(Caches.Companion.EE_SUBSCRIPTION, key = "1")
  fun delete() {
    logger.debug("Deleting local ee subscription, evicting cache.")
    val entity = findSubscriptionEntity() ?: return
    eeSubscriptionRepository.delete(entity)
  }
}
