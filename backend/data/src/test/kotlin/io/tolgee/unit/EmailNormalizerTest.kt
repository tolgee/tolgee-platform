package io.tolgee.unit

import io.tolgee.testing.assert
import io.tolgee.util.EmailNormalizer
import org.junit.jupiter.api.Test

class EmailNormalizerTest {
  @Test
  fun `strips plus alias from local part`() {
    EmailNormalizer.normalize("foo+bar@gmail.com").assert.isEqualTo("foo@gmail.com")
  }

  @Test
  fun `lowercases the whole address`() {
    EmailNormalizer.normalize("Foo@Gmail.COM").assert.isEqualTo("foo@gmail.com")
  }

  @Test
  fun `strips everything from the first plus`() {
    EmailNormalizer.normalize("a+b+c@x.com").assert.isEqualTo("a@x.com")
  }

  @Test
  fun `leaves an address without a plus unchanged`() {
    EmailNormalizer.normalize("foo@x.com").assert.isEqualTo("foo@x.com")
  }

  @Test
  fun `does not strip a plus that appears in the domain`() {
    EmailNormalizer.normalize("foo@ex+ample.com").assert.isEqualTo("foo@ex+ample.com")
  }

  @Test
  fun `does not crash on malformed input without an at sign`() {
    EmailNormalizer.normalize("NotAnEmail").assert.isEqualTo("notanemail")
  }

  @Test
  fun `extracts the lowercased domain`() {
    EmailNormalizer.domainOf("Foo@Gmail.com").assert.isEqualTo("gmail.com")
  }

  @Test
  fun `returns null domain for malformed address`() {
    EmailNormalizer.domainOf("no-at-sign").assert.isNull()
  }
}
