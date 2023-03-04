package io.tolgee.ee.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.Message
import io.tolgee.ee.EeProperties
import io.tolgee.ee.api.v2.hateoas.PrepareSetEeLicenceKeyModel
import io.tolgee.ee.api.v2.hateoas.SelfHostedEeSubscriptionModel
import io.tolgee.ee.data.ReportUsageDto
import io.tolgee.ee.data.SetLicenseKeyLicensingDto
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.security.UserAccountService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class EeSubscriptionService(
  private val eeSubscriptionRepository: EeSubscriptionRepository,
  private val restTemplate: RestTemplate,
  private val eeProperties: EeProperties,
  private val userAccountService: UserAccountService
) {
  fun getSubscription(): EeSubscription? {
    return eeSubscriptionRepository.findById(0).orElse(null)
  }

  fun setLicenceKey(licenseKey: String): EeSubscription {
    val seats = userAccountService.countAll()
    val response = try {
      postRequest<SelfHostedEeSubscriptionModel>(
        "${eeProperties.licenseServer}${eeProperties.setPath}",
        SetLicenseKeyLicensingDto(licenseKey, seats)
      )
    } catch (e: HttpClientErrorException.NotFound) {
      throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
    }

    val responseBody = response.body
    if (responseBody != null) {
      val entity = EeSubscription().apply {
        this.licenseKey = licenseKey
        this.currentPeriodEnd = responseBody.currentPeriodEnd?.let { Date(it) }
        this.enabledFeatures = responseBody.enabledFeatures
      }
      return eeSubscriptionRepository.save(entity)
    }

    throw IllegalStateException("Licence not obtained.")
  }


  fun prepareSetLicenceKey(licenseKey: String): PrepareSetEeLicenceKeyModel {
    val seats = userAccountService.countAll()


    val response = try {
      postRequest<PrepareSetEeLicenceKeyModel>(
        "${eeProperties.licenseServer}${eeProperties.prepareSetKeyPath}",
        SetLicenseKeyLicensingDto(licenseKey, seats),
      )
    } catch (e: HttpClientErrorException.NotFound) {
      throw BadRequestException(Message.LICENSE_KEY_NOT_FOUND)
    }

    val responseBody = response.body
    if (responseBody != null) {
      return responseBody
    }

    throw IllegalStateException("Licence not obtained.")
  }

  private inline fun <reified T> postRequest(url: String, body: Any): ResponseEntity<T> {
    val bodyJson = jacksonObjectMapper().writeValueAsString(body)
    val headers = HttpHeaders().apply {
      contentType = MediaType.APPLICATION_JSON
    }
    return restTemplate.exchange(
      url,
      HttpMethod.POST,
      HttpEntity(bodyJson, headers),
      T::class.java
    )
  }

  fun reportUsage() {
    val subscription = getSubscription()
    if (subscription != null) {
      val seats = userAccountService.countAll()
      postRequest<Any>(
        "${eeProperties.licenseServer}${eeProperties.reportUsagePath}",
        ReportUsageDto(subscription.licenseKey, seats)
      )
    }
  }
}

