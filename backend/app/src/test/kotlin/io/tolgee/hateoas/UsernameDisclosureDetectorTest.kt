package io.tolgee.hateoas

import io.tolgee.testing.security.UsernameDisclosureGuard
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UsernameDisclosureDetectorTest {
  // Plain classes (NOT RepresentationModel) so the guard's real classpath scan ignores them.
  private class UsernameViaConstructor(
    val username: String,
  )

  private class UsernameViaSetter {
    var username: String = ""
  }

  private class ImmutableEmptyUsername {
    @Suppress("unused")
    val username: String = ""
  }

  private class NoUsername(
    val name: String,
  )

  @Test
  fun `flags username settable via a constructor parameter`() {
    assertThat(UsernameDisclosureGuard.hasSettableUsername(UsernameViaConstructor::class)).isTrue
  }

  @Test
  fun `flags username settable via a var property`() {
    assertThat(UsernameDisclosureGuard.hasSettableUsername(UsernameViaSetter::class)).isTrue
  }

  @Test
  fun `does not flag an immutable empty val username`() {
    assertThat(UsernameDisclosureGuard.hasSettableUsername(ImmutableEmptyUsername::class)).isFalse
  }

  @Test
  fun `does not flag a model without a username property`() {
    assertThat(UsernameDisclosureGuard.hasSettableUsername(NoUsername::class)).isFalse
  }
}
