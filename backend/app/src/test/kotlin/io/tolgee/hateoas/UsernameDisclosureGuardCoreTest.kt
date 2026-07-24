package io.tolgee.hateoas

import io.tolgee.testing.security.UsernameDisclosureGuard
import org.junit.jupiter.api.Test

class UsernameDisclosureGuardCoreTest {
  @Test
  fun `no non-allowlisted core response model can carry a username`() {
    UsernameDisclosureGuard.assertNoLeak(
      UsernameDisclosureGuard.allowlistedModelNames + UsernameDisclosureGuard.corePolicedModelNames,
    )
  }
}
