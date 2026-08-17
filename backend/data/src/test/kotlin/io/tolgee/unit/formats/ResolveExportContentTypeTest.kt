package io.tolgee.unit.formats

import io.tolgee.formats.ExportFormat
import io.tolgee.formats.resolveExportContentType
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class ResolveExportContentTypeTest {
  @Test
  fun `resolves plain formats from the enum media type`() {
    resolve(ExportFormat.JSON, "en.json").assert.isEqualTo("application/json")
    resolve(ExportFormat.XLIFF, "en.xliff").assert.isEqualTo("application/x-xliff+xml")
  }

  @Test
  fun `appends utf-8 charset to text types only`() {
    resolve(ExportFormat.CSV, "en.csv").assert.isEqualTo("text/csv; charset=UTF-8")
    resolve(ExportFormat.PROPERTIES, "en.properties").assert.isEqualTo("text/plain; charset=UTF-8")
    resolve(ExportFormat.YAML, "en.yaml").assert.isEqualTo("application/x-yaml")
    resolve(ExportFormat.PO, "en.po").assert.isEqualTo("text/x-gettext-translation; charset=UTF-8")
    resolve(ExportFormat.RESX_ICU, "en.resx").assert.isEqualTo("text/microsoft-resx; charset=UTF-8")
  }

  @Test
  fun `ignores apple extensions in the path for non apple formats`() {
    resolve(ExportFormat.ANDROID_XML, "values-en/strings.xml").assert.isEqualTo("application/xml")
    resolve(ExportFormat.COMPOSE_XML, "values-en/strings.xml").assert.isEqualTo("application/xml")
    resolve(ExportFormat.JSON, "strings/en.json").assert.isEqualTo("application/json")
  }

  @Test
  fun `resolves every format to zip when zipped`() {
    ExportFormat.entries.forEach {
      resolve(it, "translations.zip", zip = true)
        .assert
        .describedAs("format %s", it)
        .isEqualTo("application/zip")
    }
  }

  @Test
  fun `resolves every format to its declared content type`() {
    val expected =
      mapOf(
        ExportFormat.JSON to "application/json",
        ExportFormat.JSON_TOLGEE to "application/json",
        ExportFormat.JSON_I18NEXT to "application/json",
        ExportFormat.XLIFF to "application/x-xliff+xml",
        ExportFormat.APPLE_XLIFF to "application/x-xliff+xml",
        ExportFormat.PO to "text/x-gettext-translation; charset=UTF-8",
        ExportFormat.APPLE_STRINGS_STRINGSDICT to "text/plain; charset=UTF-8",
        ExportFormat.ANDROID_XML to "application/xml",
        ExportFormat.COMPOSE_XML to "application/xml",
        ExportFormat.FLUTTER_ARB to "application/json",
        ExportFormat.PROPERTIES to "text/plain; charset=UTF-8",
        ExportFormat.YAML_RUBY to "application/x-yaml",
        ExportFormat.YAML to "application/x-yaml",
        ExportFormat.CSV to "text/csv; charset=UTF-8",
        ExportFormat.RESX_ICU to "text/microsoft-resx; charset=UTF-8",
        ExportFormat.XLSX to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ExportFormat.APPLE_XCSTRINGS to "application/json",
        ExportFormat.ANDROID_SDK to "application/json",
        ExportFormat.APPLE_SDK to "application/json",
      )

    expected.keys.assert
      .describedAs("every ExportFormat needs an expected content type")
      .containsExactlyInAnyOrderElementsOf(ExportFormat.entries)
    ExportFormat.entries.forEach {
      resolve(it, pathFor(it)).assert.describedAs("format %s", it).isEqualTo(expected[it])
    }
  }

  private fun pathFor(format: ExportFormat): String {
    if (format == APPLE) {
      return "en.lproj/Localizable.strings"
    }
    return "en.${format.extension}"
  }

  private fun resolve(
    format: ExportFormat,
    path: String,
    zip: Boolean = false,
  ) = resolveExportContentType(format, zip, path)

  companion object {
    private val APPLE = ExportFormat.APPLE_STRINGS_STRINGSDICT
  }
}
