package io.tolgee.security.oauth2

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

/** What the consent screen sees for a client resolved live via CIMD, rather than pre-registered. */
class OAuth2CimdConsentInfoTest : AbstractOAuth2FlowTest() {
  @MockitoBean
  @Autowired
  lateinit var cimdMetadataFetcher: CimdMetadataFetcher

  @Test
  fun `consent-info for a cimd client reports it as unverified with its origin and logo`() {
    val clientIdUrl = "https://client.example/metadata"
    val cimdClient =
      CimdClient(
        client =
          OAuth2Client(
            clientId = clientIdUrl,
            name = "Example CIMD Client",
            redirectUris = listOf("https://client.example/callback"),
            verified = false,
            metadataHash = "hash",
          ),
        logoUri = "https://client.example/logo.png",
        metadataHash = "hash",
      )
    doReturn(cimdClient).whenever(cimdMetadataFetcher).fetchAndValidate(clientIdUrl)

    val jwt = jwt()
    val pending = driver.startPendingConsent(jwt, clientIdUrl, "https://client.example/callback")
    val info = consentInfo(jwt, pending.state)

    info
      .get("verified")
      .asBoolean()
      .assert
      .isFalse()
    info
      .get("clientOrigin")
      .asString()
      .assert
      .isEqualTo("https://client.example")
    info
      .get("logoUri")
      .asString()
      .assert
      .isEqualTo("https://client.example/logo.png")
  }
}
