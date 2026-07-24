package io.tolgee.hateoas

import io.tolgee.testing.security.UsernameDisclosureGuard
import org.junit.jupiter.api.Test

/**
 * EE entrypoint for the username-disclosure guard (see [UsernameDisclosureGuard]). Runs where EE is
 * present, so it also scans EE response models and anchors a known EE model (BranchModel) to prove
 * the EE side is non-empty. The core entrypoint (`UsernameDisclosureGuardCoreTest` in `:server-app`)
 * runs the same check in every build, including EE-less ones.
 */
class UsernameDisclosureGuardTest {
  @Test
  fun `no non-allowlisted response model can carry a username`() {
    UsernameDisclosureGuard.assertNoLeak(
      UsernameDisclosureGuard.allowlistedModelNames +
        "io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel",
    )
  }
}
