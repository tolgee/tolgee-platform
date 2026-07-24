package io.tolgee.hateoas

import io.tolgee.testing.security.UsernameDisclosureGuard
import org.junit.jupiter.api.Test

/**
 * Core entrypoint for the username-disclosure guard (see [UsernameDisclosureGuard]). Lives in
 * `:server-app`, which is built and tested in EVERY build — including EE-less ones — so the core
 * response models stay guarded even when the EE test module (and its own entrypoint) is absent.
 */
class UsernameDisclosureGuardCoreTest {
  @Test
  fun `no non-allowlisted core response model can carry a username`() {
    UsernameDisclosureGuard.assertNoLeak(UsernameDisclosureGuard.allowlistedModelNames)
  }
}
