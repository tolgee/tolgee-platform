package io.tolgee.ee.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.tolgee.ee.data.OAuth2TokenResponse
import io.tolgee.service.TenantService
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.web.client.RestTemplate
import java.util.Date

class SsoMultiTenantsMocks(
  private var authMvc: MockMvc? = null,
  private val restTemplate: RestTemplate? = null,
  private val tenantService: TenantService? = null,
) {
  companion object {
    val defaultToken =
      OAuth2TokenResponse(
        id_token = generateTestJwt(jwtClaimsSet),
        scope = "scope",
        refresh_token = "refresh_token",
      )
    val defaultToken2 =
      OAuth2TokenResponse(
        id_token = generateTestJwt(jwtClaimsSet2),
        scope = "scope",
        refresh_token = "refresh_token",
      )

    val defaultTokenResponse =
      ResponseEntity(
        defaultToken,
        HttpStatus.OK,
      )
    val defaultTokenResponse2 =
      ResponseEntity(
        defaultToken2,
        HttpStatus.OK,
      )

    val jwtClaimsSet: Claims
      get() {
        return Jwts.claims().apply {
          subject = "testSubject"
          issuer = "https://test-oauth-provider.com"
          expiration = Date(System.currentTimeMillis() + 3600 * 1000)
          put("name", "Test User")
          put("given_name", "Test")
          put("given_name", "Test")
          put("family_name", "User")
          put("email", "mail@domain.com")
        }
      }

    val jwtClaimsSet2: Claims
      get() {
        return Jwts.claims().apply {
          subject = "testSubject"
          issuer = "https://test-oauth-provider.com"
          expiration = Date(System.currentTimeMillis() + 3600 * 1000)
          put("name", "Test User2")
          put("given_name", "Test2")
          put("given_name", "Test2")
          put("family_name", "User2")
          put("email", "mai2@domain.com")
        }
      }

    private fun generateTestJwt(claims: Claims): String {
      val testSecret = "test-256-bit-secretAAAAAAAAAAAAAAA"
      val key = Keys.hmacShaKeyFor(testSecret.toByteArray())
      return Jwts
        .builder()
        .setClaims(claims)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact()
    }
  }

  fun authorize(
    domain: String,
    tokenResponse: ResponseEntity<OAuth2TokenResponse>? = defaultTokenResponse,
    tokenUri: String = tenantService?.getEnabledConfigByDomain(domain)?.tokenUri!!,
  ): MvcResult {
    val receivedCode = "fake_access_token"
    // mock token exchange
    whenever(
      restTemplate?.exchange(
        eq(tokenUri),
        eq(HttpMethod.POST),
        any(),
        eq(OAuth2TokenResponse::class.java),
      ),
    ).thenReturn(tokenResponse)

    return authMvc!!
      .perform(
        MockMvcRequestBuilders.get(
          "/api/public/authorize_oauth/sso?domain=$domain&code=$receivedCode&redirect_uri=redirect_uri",
        ),
      ).andReturn()
  }

  fun getAuthLink(domain: String): MvcResult =
    authMvc!!
      .perform(
        MockMvcRequestBuilders
          .post("/api/public/authorize_oauth/sso/authentication-url")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            """
            {
                "domain": "$domain",
                "state": "state"
            }
            """.trimIndent(),
          ),
      ).andReturn()
}
