package io.tolgee.unit.util

import io.tolgee.util.PathSecurity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PathSecurityTest {
  @Test
  fun `keeps normal paths unchanged`() {
    assertThat(PathSecurity.sanitizePath("screenshots/test.png")).isEqualTo("screenshots/test.png")
    assertThat(PathSecurity.sanitizePath("a/b/c.txt")).isEqualTo("a/b/c.txt")
  }

  @Test
  fun `strips single leading traversal`() {
    assertThat(PathSecurity.sanitizePath("../etc/passwd")).isEqualTo("etc/passwd")
  }

  @Test
  fun `strips multiple leading traversals`() {
    assertThat(PathSecurity.sanitizePath("../../etc/passwd")).isEqualTo("etc/passwd")
    assertThat(PathSecurity.sanitizePath("../../../bar")).isEqualTo("bar")
  }

  @Test
  fun `normalizes mid-path traversal`() {
    assertThat(PathSecurity.sanitizePath("a/b/../../c.txt")).isEqualTo("c.txt")
  }

  @Test
  fun `returns empty for bare dot-dot`() {
    assertThat(PathSecurity.sanitizePath("..")).isEqualTo("")
  }

  @Test
  fun `handles redundant separators`() {
    assertThat(PathSecurity.sanitizePath("a//b/../c")).isEqualTo("a/c")
  }

  @Test
  fun `handles dot segments`() {
    assertThat(PathSecurity.sanitizePath("./a/./b")).isEqualTo("a/b")
  }
}
