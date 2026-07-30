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
  private val executionAnnotations =
    listOf("@Test", "@ParameterizedTest", "@RepeatedTest", "@TestFactory", "@TestTemplate")

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
          if (!line.trim().startsWith("@ProjectJWTAuthTestMethod")) return@forEachIndexed
          usages++
          if (!annotationBlockAround(lines, index).any { block -> executionAnnotations.any { block.startsWith(it) } }) {
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

  private fun annotationBlockAround(
    lines: List<String>,
    at: Int,
  ): List<String> {
    val block = mutableListOf<String>()
    var i = at
    while (i >= 0 && isAnnotationOrBlank(lines[i])) {
      block += lines[i].trim()
      i--
    }
    i = at + 1
    while (i < lines.size && isAnnotationOrBlank(lines[i])) {
      block += lines[i].trim()
      i++
    }
    return block
  }

  private fun isAnnotationOrBlank(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("@") || trimmed.startsWith("//") || trimmed.isEmpty()
  }

  /** Modules carry their own settings.gradle, so the outermost one is the repository root. */
  private fun repositoryRoot(): File {
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
