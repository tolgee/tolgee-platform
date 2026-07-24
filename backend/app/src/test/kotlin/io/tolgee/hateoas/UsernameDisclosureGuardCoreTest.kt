package io.tolgee.hateoas

import io.tolgee.testing.security.UsernameDisclosureGuard
import org.junit.jupiter.api.Test

/** Core entrypoint for [UsernameDisclosureGuard], in `:server-app` so it runs in every build (incl. EE-less). */
class UsernameDisclosureGuardCoreTest {
  @Test
  fun `no non-allowlisted core response model can carry a username`() {
    UsernameDisclosureGuard.assertNoLeak(
      UsernameDisclosureGuard.allowlistedModelNames + UsernameDisclosureGuard.corePolicedModelNames,
    )
  }
}
