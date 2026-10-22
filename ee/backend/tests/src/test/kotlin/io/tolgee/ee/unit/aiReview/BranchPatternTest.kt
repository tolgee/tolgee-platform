package io.tolgee.ee.unit.aiReview

import io.tolgee.ee.service.aiReview.matching.BranchPattern
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class BranchPatternTest {
  @Test
  fun `star matches one path level`() {
    BranchPattern.matches("feature/*", "feature/a").assert.isTrue()
  }

  @Test
  fun `star matches across slashes`() {
    BranchPattern.matches("feature/*", "feature/a/b").assert.isTrue()
  }

  @Test
  fun `matching is case-sensitive`() {
    BranchPattern.matches("feature/*", "Feature/a").assert.isFalse()
  }

  @Test
  fun `pattern must match the whole name`() {
    BranchPattern.matches("feature/*", "xfeature/a").assert.isFalse()
  }

  @Test
  fun `plain name matches only itself`() {
    BranchPattern.matches("main", "main").assert.isTrue()
    BranchPattern.matches("main", "main2").assert.isFalse()
  }

  @Test
  fun `single star matches everything`() {
    BranchPattern.matches("*", "release/2026").assert.isTrue()
  }

  @Test
  fun `regex characters match literally`() {
    BranchPattern.matches("release.1", "release.1").assert.isTrue()
    BranchPattern.matches("release.1", "releaseX1").assert.isFalse()
    BranchPattern.matches("a+b", "a+b").assert.isTrue()
    BranchPattern.matches("a+b", "aab").assert.isFalse()
  }
}
