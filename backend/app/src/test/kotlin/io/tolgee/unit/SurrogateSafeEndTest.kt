package io.tolgee.unit

import io.tolgee.testing.assert
import io.tolgee.util.surrogateSafeEnd
import org.junit.jupiter.api.Test

class SurrogateSafeEndTest {
  @Test
  fun `it steps back off a lone high surrogate`() {
    surrogateSafeEnd("ab😀", 3).assert.isEqualTo(2)
  }

  @Test
  fun `it leaves a cut that lands between characters alone`() {
    surrogateSafeEnd("abcd", 3).assert.isEqualTo(3)
  }

  @Test
  fun `it keeps a whole surrogate pair when the cut falls after it`() {
    surrogateSafeEnd("a😀b", 3).assert.isEqualTo(3)
  }

  @Test
  fun `it never returns an index outside the name`() {
    surrogateSafeEnd("ab", 3).assert.isEqualTo(2)
    surrogateSafeEnd("", 3).assert.isEqualTo(0)
    surrogateSafeEnd("abc", 0).assert.isEqualTo(0)
    surrogateSafeEnd("abc", -1).assert.isEqualTo(0)
  }
}
