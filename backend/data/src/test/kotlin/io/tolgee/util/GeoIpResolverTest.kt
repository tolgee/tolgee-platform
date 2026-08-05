package io.tolgee.util

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.SessionAuditProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class GeoIpResolverTest {
  /**
   * The address comes from forwarding headers, so anything that is not an IP literal must be
   * rejected before it reaches `InetAddress.getByName` - that method resolves hostnames, and an
   * out-of-range dotted string like `999.999.999.999` counts as one.
   */
  @Test
  fun `never resolves a value that is not an ip literal`() {
    val resolver = resolverWithoutDatabase()

    listOf(
      "evil.example.com",
      "999.999.999.999",
      "256.1.1.1",
      "1.2.3",
      "1.2.3.4.5",
      "1.2.3.four",
      // contains a colon but is not a literal - the JDK only short-circuits on the first character,
      // so these measurably reach the resolver without the guard
      "x:1",
      "zevil.example.com:80",
      "www.google.com:1",
      "",
      "   ",
    ).forEach { value ->
      resolver.resolve(value).assert.isNull()
    }
  }

  @Test
  fun `returns null instead of failing when no database is configured`() {
    val resolver = resolverWithoutDatabase()

    resolver.resolve("8.8.8.8").assert.isNull()
    resolver.resolve("2001:4860:4860::8888").assert.isNull()
    resolver.resolve(null).assert.isNull()
  }

  private fun resolverWithoutDatabase(): GeoIpResolver {
    val properties =
      TolgeeProperties().apply {
        authentication = AuthenticationProperties().apply { sessionAudit = SessionAuditProperties() }
      }
    return GeoIpResolver(properties)
  }
}
