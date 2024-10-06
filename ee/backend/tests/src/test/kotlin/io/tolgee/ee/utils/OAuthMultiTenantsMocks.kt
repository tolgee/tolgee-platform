package io.tolgee.ee.utils

import io.tolgee.ee.repository.DynamicOAuth2ClientRegistrationRepository
import io.tolgee.ee.service.OAuthService
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.client.RestTemplate

class OAuthMultiTenantsMocks(
  private var authMvc: MockMvc? = null,
  private val restTemplate: RestTemplate? = null,
  private val dynamicOAuth2ClientRegistrationRepository: DynamicOAuth2ClientRegistrationRepository,
) {
  companion object {
    val defaultUserResponse =
      OAuthService.GenericUserResponse().apply {
        sub = "fakeId"
        given_name = "fakeGiveName"
        family_name = "fakeGivenFamilyName"
        email = "email@domain.com"
      }

    val defaultToken =
      OAuthService.OAuth2TokenResponse(id_token = "id_token", scope = "scope")

    val defaultTokenResponse =
      ResponseEntity(
        defaultToken,
        HttpStatus.OK,
      )
  }

  fun authorize(registrationId: String) {
    val receivedCode = "fake_access_token"
    val registration = dynamicOAuth2ClientRegistrationRepository.findByRegistrationId(registrationId).clientRegistration

    whenever(
      restTemplate?.exchange(
        eq(registration.providerDetails.tokenUri),
        eq(HttpMethod.POST),
        any(),
        eq(OAuthService.OAuth2TokenResponse::class.java),
      ),
    ).thenReturn(defaultTokenResponse)
  }
}
