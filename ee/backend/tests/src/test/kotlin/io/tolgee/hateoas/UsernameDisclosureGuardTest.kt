package io.tolgee.hateoas

import io.tolgee.testing.security.UsernameDisclosureGuard
import org.junit.jupiter.api.Test

class UsernameDisclosureGuardTest {
  @Test
  fun `no non-allowlisted response model can carry a username`() {
    UsernameDisclosureGuard.assertNoLeak(
      UsernameDisclosureGuard.allowlistedModelNames +
        UsernameDisclosureGuard.corePolicedModelNames +
        "io.tolgee.ee.api.v2.hateoas.model.branching.BranchModel",
    )
  }
}
