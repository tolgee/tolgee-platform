package io.tolgee.security.oauth2.cimd

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

/**
 * Fail-closed validation of a Client ID Metadata Document. Every rule that is not met drops the whole document to
 * null — a hostile field never merely narrows the client it would otherwise build.
 */
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
    result.logoUri.assert.isEqualTo("https://app.example.com/logo.png")
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
  fun `a cross-origin logo is dropped, not rejected`() {
    val result = fetcher.buildClient(clientId, document(logoUri = "https://cdn.other.example/logo.png"))

    result.assert.isNotNull()
    result!!.logoUri.assert.isNull()
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
  fun `the metadata hash changes when the document changes`() {
    val a = fetcher.buildClient(clientId, document())!!.client.metadataHash
    val b = fetcher.buildClient(clientId, document(clientName = "Renamed App"))!!.client.metadataHash

    a.assert.isNotEqualTo(b)
  }

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
