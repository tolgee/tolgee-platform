package io.tolgee.ee.service.eeSubscription

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Message
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.service.NoActiveSubscriptionException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.util.executeInNewTransaction
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.client.HttpClientErrorException

@Component
class EeSubscriptionErrorCatchingService(
  private val transactionManager: PlatformTransactionManager,
  @Lazy
  private val eeSubscriptionService: EeSubscriptionServiceImpl
) {
  fun <T> catchingSpendingLimits(fn: () -> T): T {
    return try {
      fn()
    } catch (e: HttpClientErrorException.BadRequest) {
      val body = e.parseBody()
      // TODO: TEST THIS
      when (body.code) {
        Message.SEATS_SPENDING_LIMIT_EXCEEDED.code,
        Message.KEYS_SPENDING_LIMIT_EXCEEDED.code,
        Message.PLAN_KEY_LIMIT_EXCEEDED.code,
        Message.PLAN_SEAT_LIMIT_EXCEEDED.code ->
          throw BadRequestException(body.code, body.params)
      }
      throw e
    }
  }

  fun <T> catchingLicenseNotFound(fn: () -> T): T {
    try {
      return fn()
    } catch (e: HttpClientErrorException.NotFound) {
      executeInNewTransaction(transactionManager) {
        val entity = eeSubscriptionService.findSubscriptionEntity() ?: throw NoActiveSubscriptionException()
        entity.status = SubscriptionStatus.CANCELED
        eeSubscriptionService.save(entity)
        throw e
      }
    }
  }

  fun <T> catchingLicenseUsedByAnotherInstance(fn: () -> T): T? {
    try {
      return fn()
    } catch (e: HttpClientErrorException.NotFound) {
      val subscription = eeSubscriptionService.findSubscriptionEntity()
      subscription?.status = SubscriptionStatus.CANCELED
      return null
    } catch (e: HttpClientErrorException.BadRequest) {
      val error = e.parseBody()
      if (error.code == Message.LICENSE_KEY_USED_BY_ANOTHER_INSTANCE.code) {
        setSubscriptionKeyUsedByOtherInstance()
      }
      return null
    }
  }

  private fun HttpClientErrorException.parseBody(): ErrorResponseBody {
    return jacksonObjectMapper().readValue(this.responseBodyAsString, ErrorResponseBody::class.java)
  }

  private fun setSubscriptionKeyUsedByOtherInstance() {
    val subscription = eeSubscriptionService.findSubscriptionEntity() ?: return
    subscription.status = SubscriptionStatus.KEY_USED_BY_ANOTHER_INSTANCE
    eeSubscriptionService.save(subscription)
  }
}
