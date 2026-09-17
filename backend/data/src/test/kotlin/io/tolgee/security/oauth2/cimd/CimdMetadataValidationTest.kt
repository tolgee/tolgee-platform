package io.tolgee.security.oauth2.cimd

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class CimdMetadataValidationTest {
  private val fetcher = CimdMetadataFetcher(mock())

  private val clientId = "https://app.example.com/.well-known/oauth-client"

  @Test
  fun `a well-formed document becomes an unverified client`() {
    val result = fetcher.buildClient(clientId, document())

    result.assert.isNotNull()
    result!!
      .client.clientId.assert
      .isEqualTo(clientId)
    result.client.verified.assert
      .isFalse()
    result.client.metadataHash.assert
      .isNotNull()
    result.client.name.assert
      .isEqualTo("Example App")
    result.client.redirectUris.assert
      .containsExactly("https://app.example.com/callback")
    result.clientOrigin.assert.isEqualTo("https://app.example.com")
  }

  @Test
  fun `a loopback redirect is accepted for a native client`() {
    val result = fetcher.buildClient(clientId, document(redirectUris = """["http://127.0.0.1:8123/callback"]"""))

    result.assert.isNotNull()
    result!!
      .client.redirectUris.assert
      .containsExactly("http://127.0.0.1:8123/callback")
  }

  @Test
  fun `the document's client_id must equal the fetched URL`() {
    fetcher.buildClient(clientId, document(clientIdField = "\"https://evil.example/other\"")).assert.isNull()
    fetcher.buildClient(clientId, document(clientIdField = null)).assert.isNull()
  }

  @Test
  fun `a confidential-client auth method is refused`() {
    fetcher.buildClient(clientId, document(authMethod = "client_secret_basic")).assert.isNull()
    fetcher.buildClient(clientId, document(authMethod = null)).assert.isNull()
  }

  @Test
  fun `grant_types may be absent but must contain authorization_code when present`() {
    fetcher.buildClient(clientId, document(grantTypes = null)).assert.isNotNull()
    fetcher
      .buildClient(
        clientId,
        document(grantTypes = """["authorization_code","refresh_token"]"""),
      ).assert
      .isNotNull()
    fetcher.buildClient(clientId, document(grantTypes = """["client_credentials"]""")).assert.isNull()
    fetcher.buildClient(clientId, document(grantTypes = """"authorization_code"""")).assert.isNull()
  }

  @Test
  fun `redirect_uris must be present and non-empty`() {
    fetcher.buildClient(clientId, document(redirectUris = "[]")).assert.isNull()
    fetcher.buildClient(clientId, document(redirectUris = null)).assert.isNull()
    fetcher.buildClient(clientId, document(redirectUris = """"https://app.example.com/callback"""")).assert.isNull()
  }

  @Test
  fun `a cross-origin or non-https redirect is refused`() {
    fetcher.buildClient(clientId, document(redirectUris = """["https://other.example/callback"]""")).assert.isNull()
    fetcher.buildClient(clientId, document(redirectUris = """["http://app.example.com/callback"]""")).assert.isNull()
    fetcher
      .buildClient(
        clientId,
        document(redirectUris = """["https://app.example.com:8443/callback"]"""),
      ).assert
      .isNull()
  }

  @Test
  fun `a redirect on the explicit default https port is same-origin with an implicit-port client_id`() {
    val result = fetcher.buildClient(clientId, document(redirectUris = """["https://app.example.com:443/callback"]"""))

    result.assert.isNotNull()
    result!!
      .client.redirectUris.assert
      .containsExactly("https://app.example.com:443/callback")
  }

  @Test
  fun `a redirect with a fragment is refused`() {
    fetcher.buildClient(clientId, document(redirectUris = """["https://app.example.com/cb#x"]""")).assert.isNull()
    fetcher.buildClient(clientId, document(redirectUris = """["http://127.0.0.1:8123/cb#x"]""")).assert.isNull()
  }

  @Test
  fun `one hostile redirect rejects the whole set rather than being dropped`() {
    val mixed = """["https://app.example.com/callback","https://evil.example/steal"]"""

    fetcher.buildClient(clientId, document(redirectUris = mixed)).assert.isNull()
  }

  @Test
  fun `a non-string redirect element rejects the document`() {
    fetcher
      .buildClient(
        clientId,
        document(redirectUris = """["https://app.example.com/callback", 42]"""),
      ).assert
      .isNull()
    fetcher
      .buildClient(
        clientId,
        document(redirectUris = """[{"uri":"https://app.example.com/callback"}]"""),
      ).assert
      .isNull()
  }

  @Test
  fun `a missing client_name falls back to the client_id origin`() {
    val result = fetcher.buildClient(clientId, document(clientName = null))

    result.assert.isNotNull()
    result!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
  }

  @Test
  fun `a container where a string is expected is refused, not coerced`() {
    fetcher.buildClient(clientId, document(clientIdField = "{}")).assert.isNull()
  }

  @Test
  fun `unparseable or empty input yields no client`() {
    fetcher.buildClient(clientId, "not json").assert.isNull()
    fetcher.buildClient(clientId, "").assert.isNull()
    fetcher.buildClient(clientId, "[]").assert.isNull()
  }

  @Test
  fun `a loopback redirect on a scheme other than http or https is refused`() {
    fetcher.buildClient(clientId, document(redirectUris = """["javascript://127.0.0.1/cb"]""")).assert.isNull()
    fetcher.buildClient(clientId, document(redirectUris = """["ftp://localhost:8123/cb"]""")).assert.isNull()
  }

  @Test
  fun `an over-long client_name falls back to the origin instead of being rendered`() {
    val result = fetcher.buildClient(clientId, document(clientName = "A".repeat(101)))

    result.assert.isNotNull()
    result!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
  }

  @Test
  fun `a client_name carrying formatting control characters falls back to the origin`() {
    // Newlines and bidi overrides would let the name reorder or push aside the unverified warning beside it.
    fetcher
      .buildClient(clientId, document(clientName = """Tolgee\u202eOfficial"""))!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
    fetcher
      .buildClient(clientId, document(clientName = """Tolgee\nOfficial"""))!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
  }

  @Test
  fun `a client_name carrying any other invisible format character falls back to the origin`() {
    // U+061C is a bidi mark and U+FEFF a zero-width space: format characters, not controls.
    fetcher
      .buildClient(clientId, document(clientName = """Tolgee\u061cOfficial"""))!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
    fetcher
      .buildClient(clientId, document(clientName = """Tolgee\ufeffOfficial"""))!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
  }

  @Test
  fun `a client_name of stacked combining marks falls back to the origin`() {
    fetcher
      .buildClient(clientId, document(clientName = "A" + "\\u0301".repeat(60)))!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
  }

  @Test
  fun `a client_name of combining marks outside the basic plane falls back to the origin`() {
    // U+1D167 is two UTF-16 units, so 40 of them stack 40 marks and still sit under the length cap.
    fetcher
      .buildClient(clientId, document(clientName = "A" + "\\ud834\\udd67".repeat(40)))!!
      .client.name.assert
      .isEqualTo("https://app.example.com")
  }

  @Test
  fun `the metadata hash changes when the consented terms change`() {
    val unedited = fetcher.buildClient(clientId, document())!!.client.metadataHash
    val redirects = fetcher.buildClient(clientId, document(redirectUris = """["https://app.example.com/other"]"""))

    unedited.assert.isNotEqualTo(redirects!!.client.metadataHash)
  }

  @Test
  fun `a loopback redirect's port is not a consented term, since it is not one the authorize path matches on`() {
    val onePort = hashOfLoopbackRedirect("http://127.0.0.1:1234/cb")

    onePort.assert.isEqualTo(hashOfLoopbackRedirect("http://127.0.0.1:9999/cb"))
    onePort.assert.isNotEqualTo(hashOfLoopbackRedirect("http://127.0.0.1:1234/other"))
  }

  @Test
  fun `a loopback redirect carrying a query does keep its port, since one presented URI turns on it`() {
    hashOfLoopbackRedirect("http://127.0.0.1:1234/cb?x=1")
      .assert
      .isNotEqualTo(hashOfLoopbackRedirect("http://127.0.0.1:9999/cb?x=1"))
  }

  @Test
  fun `a grant type the authorize path never reads is not a consented term`() {
    val unedited = fetcher.buildClient(clientId, document())!!.client.metadataHash
    val declared = document(grantTypes = """["authorization_code","refresh_token"]""")

    unedited.assert.isEqualTo(fetcher.buildClient(clientId, declared)!!.client.metadataHash)
  }

  @Test
  fun `the metadata hash survives a cosmetic edit, so a publisher's reformatting does not revoke every grant`() {
    val unedited = fetcher.buildClient(clientId, document())!!.client.metadataHash
    val renamed = fetcher.buildClient(clientId, document(clientName = "Renamed App"))!!.client.metadataHash
    val relogoed =
      fetcher.buildClient(clientId, document(logoUri = "https://app.example.com/new.png"))!!.client.metadataHash
    val reordered =
      fetcher
        .buildClient(
          clientId,
          """{"redirect_uris": ["https://app.example.com/callback"], "grant_types": ["authorization_code"],
           "token_endpoint_auth_method": "none", "client_id": "$clientId", "client_name": "Example App"}""",
        )!!
        .client.metadataHash

    val absentGrantTypes = fetcher.buildClient(clientId, document(grantTypes = null))!!.client.metadataHash

    unedited.assert.isEqualTo(absentGrantTypes)
    unedited.assert.isEqualTo(renamed)
    unedited.assert.isEqualTo(relogoed)
    unedited.assert.isEqualTo(reordered)
  }

  @Test
  fun `a redirect set past the cap, or one entry past the URI cap, rejects the document`() {
    val within = (1..CimdMetadataFetcher.MAX_REDIRECT_URIS).joinToString(",") { """"https://app.example.com/cb$it"""" }
    val over =
      (1..CimdMetadataFetcher.MAX_REDIRECT_URIS + 1).joinToString(",") { """"https://app.example.com/cb$it"""" }
    val longUri = "https://app.example.com/" + "p".repeat(CimdMetadataFetcher.MAX_URI_LENGTH)

    fetcher.buildClient(clientId, document(redirectUris = "[$within]")).assert.isNotNull()
    fetcher.buildClient(clientId, document(redirectUris = "[$over]")).assert.isNull()
    fetcher.buildClient(clientId, document(redirectUris = """["$longUri"]""")).assert.isNull()
  }

  @Test
  fun `a logo_uri is neither read nor a reason to refuse the document`() {
    val longLogo = "https://app.example.com/" + "p".repeat(CimdMetadataFetcher.MAX_URI_LENGTH)

    fetcher.buildClient(clientId, document(logoUri = longLogo)).assert.isNotNull()
    fetcher
      .buildClient(clientId, document(logoUri = "https://cdn.other.example/logo.png"))
      .assert
      .isNotNull()
  }

  @Test
  fun `a client_id whose host differs only in case is same-origin with its redirects, and the origin is lowercased`() {
    val mixedCase = "https://App.Example.com/.well-known/oauth-client"

    val result =
      fetcher.buildClient(
        mixedCase,
        """{"client_id":"$mixedCase","client_name":"Example App","token_endpoint_auth_method":"none",
           "redirect_uris":["https://app.example.com/callback"]}""",
      )

    result.assert.isNotNull()
    result!!.clientOrigin.assert.isEqualTo("https://app.example.com")
  }

  private fun hashOfLoopbackRedirect(uri: String): String =
    fetcher.buildClient(clientId, document(redirectUris = """["$uri"]"""))!!.client.metadataHash!!

  private fun document(
    clientIdField: String? = "\"$clientId\"",
    clientName: String? = "Example App",
    authMethod: String? = "none",
    grantTypes: String? = """["authorization_code"]""",
    redirectUris: String? = """["https://app.example.com/callback"]""",
    logoUri: String? = "https://app.example.com/logo.png",
  ): String {
    // `clientIdField` is a JSON fragment already: a quoted string, an object literal, or null to omit it.
    val fields = mutableListOf<String>()
    clientIdField?.let { fields += "\"client_id\": $it" }
    clientName?.let { fields += "\"client_name\": \"$it\"" }
    authMethod?.let { fields += "\"token_endpoint_auth_method\": \"$it\"" }
    grantTypes?.let { fields += "\"grant_types\": $it" }
    redirectUris?.let { fields += "\"redirect_uris\": $it" }
    logoUri?.let { fields += "\"logo_uri\": \"$it\"" }
    return "{${fields.joinToString(",")}}"
  }
}
