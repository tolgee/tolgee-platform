package io.tolgee.unit

import io.tolgee.testing.assert
import io.tolgee.util.urlHostOnly
import org.junit.jupiter.api.Test

class UrlHostOnlyTest {
  @Test
  fun `drops userinfo, port, path and query`() {
    urlHostOnly("https://user@example.com:8443/path?param=value#fragment").assert.isEqualTo("example.com")
  }

  @Test
  fun `returns null instead of the raw value when the url cannot be parsed`() {
    urlHostOnly("https://exa mple.com/path").assert.isNull()
    urlHostOnly("no-scheme").assert.isNull()
  }

  @Test
  fun `returns null for a value that is not a string`() {
    urlHostOnly(42).assert.isNull()
    urlHostOnly(null).assert.isNull()
  }
}
