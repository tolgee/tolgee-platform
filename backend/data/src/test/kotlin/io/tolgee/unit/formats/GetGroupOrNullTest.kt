package io.tolgee.unit.formats

import io.tolgee.formats.getGroupOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Recognises an absent named group from the text of the stdlib's IllegalArgumentException, so a
 * reword turns every C-like placeholder conversion into a thrown exception rather than a null.
 */
class GetGroupOrNullTest {
  private val match = Regex("(?<present>x)").find("x")!!

  @Test
  fun `returns null for a group the pattern does not declare`() {
    assertThat(match.groups.getGroupOrNull("absent")).isNull()
  }

  @Test
  fun `returns the group the pattern does declare`() {
    assertThat(match.groups.getGroupOrNull("present")?.value).isEqualTo("x")
  }
}
