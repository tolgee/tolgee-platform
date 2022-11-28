package io.tolgee.ee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.PlanPricesModel
import io.tolgee.ee.api.v2.hateoas.PrepareSetEeLicenceKeyModel
import io.tolgee.ee.api.v2.hateoas.SelfHostedEePlanModel
import io.tolgee.ee.api.v2.hateoas.SelfHostedEeSubscriptionModel
import io.tolgee.ee.api.v2.hateoas.uasge.MeteredUsageModel
import io.tolgee.ee.api.v2.hateoas.uasge.ProportionalUsageItemModel
import io.tolgee.ee.data.SubscriptionStatus
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class EeLicensingMockRequestUtil {
  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  fun mock(mock: Mocker.() -> Unit) {
    val mocker = Mocker(restTemplate)
    mock(mocker)
  }

  final val mockedPlan = SelfHostedEePlanModel(
    id = 19919,
    name = "Tolgee",
    public = true,
    enabledFeatures = arrayOf(Feature.PREMIUM_SUPPORT),
    prices = PlanPricesModel(
      perSeat = 20.toBigDecimal(),
      subscriptionMonthly = 200.toBigDecimal(),
    ),
  )

  final val mockedSubscriptionResponse = SelfHostedEeSubscriptionModel(
    id = 19919,
    currentPeriodEnd = 1624313600000,
    createdAt = 1624313600000,
    plan = mockedPlan,
    status = SubscriptionStatus.ACTIVE,
    licenseKey = "mocked_license_key",
    estimatedCosts = 200.toBigDecimal(),
    currentPeriodStart = 1622313600000,
  )

  final val mockedPrepareResponse = PrepareSetEeLicenceKeyModel().apply {
    plan = mockedPlan
    usage = MeteredUsageModel(
      subscriptionPrice = 200.toBigDecimal(),
      seatsPeriods = listOf(
        ProportionalUsageItemModel(
          from = 1624313600000,
          to = 1624313600000,
          total = 250.toBigDecimal(),
          usedQuantity = 2,
          unusedQuantity = 10,
          usedQuantityOverPlan = 0
        )
      ),
      total = 250.toBigDecimal(),
      translationsPeriods = listOf(),
      credits = null
    )
  }

  class Mocker(private val restTemplate: RestTemplate) {
    data class VerifyTools(
      val captor: KArgumentCaptor<HttpEntity<*>> = argumentCaptor()
    )

    data class Definition(
      var url: (String) -> Boolean = { true },
      var method: (HttpMethod) -> Boolean = { true },
    )

    private lateinit var definition: Definition
    private var answer: (() -> ResponseEntity<String>)? = null
    private var toThrow: Throwable? = null

    private val verifyTools: VerifyTools = VerifyTools()

    private fun updateWhenever() {
      whenever(
        restTemplate.exchange(
          argThat<String> { definition.url(this) },
          argThat { definition.method(this) },
          verifyTools.captor.capture(),
          eq(String::class.java)
        )
      ).apply {
        if (toThrow != null) {
          thenThrow(toThrow!!)
        }

        if (answer != null) {
          thenAnswer { answer!!() }
        }
      }
    }

    fun whenReq(fn: Definition.() -> Unit) {
      definition = Definition().apply(fn)
    }

    fun thenAnswer(response: () -> Any) {
      answer = {
        ResponseEntity(jacksonObjectMapper().writeValueAsString(response()), HttpStatus.OK)
      }
      updateWhenever()
    }

    fun thenThrow(throwable: Throwable) {
      toThrow = throwable
      updateWhenever()
    }

    fun verify(fn: VerifyTools.() -> Unit) {
      verifyTools.fn()
    }
  }
}
