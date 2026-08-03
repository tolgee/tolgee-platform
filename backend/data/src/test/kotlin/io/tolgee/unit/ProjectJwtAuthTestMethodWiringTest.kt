package io.tolgee.unit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * `@ProjectJWTAuthTestMethod` is not meta-annotated with `@Test` — it cannot be, because
 * `V2ExportAllFormatsTest.it exports to all formats` combines it with `@ParameterizedTest` and JUnit
 * cannot resolve a method that is both. So a method carrying only `@ProjectJWTAuthTestMethod` is never
 * discovered and never runs, which is how `CreditLimitTest.correctly propagates credit spending limit
 * exceeded` sat dead in the repository. Its sibling `@ProjectApiKeyAuthTestMethod` does carry `@Test`,
 * so the same omission there is harmless — the asymmetry is what makes this scan necessary.
 *
 * Scans source rather than a classpath because the usages span :app, :ee-test and :data test sets, and
 * reflection from one module sees only its own.
 */
class ProjectJwtAuthTestMethodWiringTest {
  private val usage = Regex("@ProjectJWTAuthTestMethod\\b")
  private val executionAnnotations =
    listOf("@Test", "@ParameterizedTest", "@RepeatedTest", "@TestFactory", "@TestTemplate")
  private val declaration =
    Regex("^(private |internal |protected |public |open |override |abstract )*(fun|class|object) ")

  @Test
  fun `every method annotated with ProjectJWTAuthTestMethod can actually run`() {
    val offenders = mutableListOf<String>()
    var usages = 0

    repositoryRoot()
      .walkTopDown()
      .filter { it.isFile && it.extension == "kt" }
      .filter { it.path.contains("/src/test/") && !it.path.contains("/build/") }
      .forEach { file ->
        val lines = file.readLines()
        lines.forEachIndexed { index, line ->
          // Must be an annotation line, not a mention of the name in a string or comment — this file
          // names it in its own assertion messages.
          if (!line.trim().startsWith("@") || !usage.containsMatchIn(line)) return@forEachIndexed
          usages++
          if (!annotationBlockAround(lines, index).any { block -> executionAnnotations.any { block.contains(it) } }) {
            offenders += "${file.name}:${index + 1}"
          }
        }
      }

    // A scan that stops seeing the repository would report no offenders and pass, which is the same
    // silent failure it exists to catch — so a floor well under the current count guards the scan itself.
    assertThat(usages)
      .describedAs("@ProjectJWTAuthTestMethod usages found; a collapse means repositoryRoot() resolved wrongly")
      .isGreaterThan(300)
    assertThat(offenders)
      .describedAs("methods that carry @ProjectJWTAuthTestMethod but no annotation JUnit executes")
      .isEmpty()
  }

  /**
   * Collects the annotations attached to the declaration below [at]. Walks up to the end of the
   * previous declaration rather than stopping at the first line that is not an annotation, so the
   * continuation lines of a multi-line annotation do not cut the block short.
   */
  private fun annotationBlockAround(
    lines: List<String>,
    at: Int,
  ): List<String> {
    val block = mutableListOf<String>()
    var above = at
    while (above >= 0 && !endsTheBlock(lines[above].trim())) {
      block += lines[above].trim()
      above--
    }
    var below = at + 1
    while (below < lines.size && !endsTheBlock(lines[below].trim())) {
      block += lines[below].trim()
      below++
    }
    return block
  }

  private fun endsTheBlock(line: String) = line == "}" || declaration.containsMatchIn(line)

  /** Modules carry their own settings.gradle, so the outermost one is the repository root. */
  private fun repositoryRoot(): File {
    System.getProperty("jwtAuthWiringScanRoot")?.let { return File(it) }
    var candidate: File? = File(System.getProperty("user.dir")).absoluteFile
    var outermost: File? = null
    while (candidate != null) {
      if (File(candidate, "settings.gradle").exists()) {
        outermost = candidate
      }
      candidate = candidate.parentFile
    }
    return requireNotNull(outermost) { "no settings.gradle above ${System.getProperty("user.dir")}" }
  }
}
