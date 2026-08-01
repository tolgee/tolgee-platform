package io.tolgee.unit.formats.apple.out

import io.tolgee.formats.apple.out.getAppleExportPathContentType
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class GetAppleExportPathContentTypeTest {
  @Test
  fun `resolves apple strings and stringsdict by extension`() {
    appleContentType("en.lproj/Localizable.strings").assert.isEqualTo("text/plain")
    appleContentType("en.lproj/Localizable.stringsdict").assert.isEqualTo("application/xml")
  }

  @Test
  fun `prefers the file name over a directory segment carrying a separator`() {
    appleContentType("a.strings/Localizable.stringsdict").assert.isEqualTo("application/xml")
    appleContentType("a.stringsdict/Localizable.strings").assert.isEqualTo("text/plain")
  }

  @Test
  fun `resolves nothing when the path carries no apple extension`() {
    appleContentType("en.lproj/Localizable").assert.isNull()
  }

  @Test
  fun `resolves apple extension when the template puts a suffix after it`() {
    appleContentType("en.strings.txt").assert.isEqualTo("text/plain")
    appleContentType("en.lproj/Localizable.stringsdict.bak").assert.isEqualTo("application/xml")
  }

  @Test
  fun `resolves apple extension when the separator before it is not a letter`() {
    appleContentType("en_strings").assert.isEqualTo("text/plain")
    appleContentType("en_stringsdict").assert.isEqualTo("application/xml")
  }

  @Test
  fun `does not resolve an apple extension that is part of a longer word`() {
    appleContentType("mystrings").assert.isNull()
    appleContentType("strings2").assert.isNull()
    appleContentType("v2strings").assert.isNull()
    appleContentType("stringsdictionary").assert.isNull()
  }

  @Test
  fun `matches the apple extension case-sensitively`() {
    appleContentType("Strings").assert.isNull()
  }

  @Test
  fun `resolves apple extension from a directory segment`() {
    appleContentType("strings/en").assert.isEqualTo("text/plain")
    appleContentType("stringsdict/en").assert.isEqualTo("application/xml")
  }

  @Test
  fun `refuses to guess when two directories are both an exact extension`() {
    appleContentType("strings/stringsdict/en").assert.isNull()
    appleContentType("stringsdict/strings/en").assert.isNull()
  }

  @Test
  fun `prefers a whole directory segment over a namespace merely containing the word`() {
    appleContentType("stringsdict/ios-strings/en").assert.isEqualTo("application/xml")
    appleContentType("strings/my-stringsdict-ns/en").assert.isEqualTo("text/plain")
  }

  @Test
  fun `resolves a separator-less extension rendered into a directory`() {
    appleContentType("en_strings/Localizable").assert.isEqualTo("text/plain")
    appleContentType("en_stringsdict/Localizable").assert.isEqualTo("application/xml")
  }

  @Test
  fun `prefers the file name's own extension over a directory named like one`() {
    appleContentType("strings/en_stringsdict").assert.isEqualTo("application/xml")
    appleContentType("stringsdict/en_strings").assert.isEqualTo("text/plain")
  }

  @Test
  fun `prefers the file name over directory segments`() {
    appleContentType("stringsdict/en.lproj/Localizable.strings").assert.isEqualTo("text/plain")
  }

  @Test
  fun `prefers a dot-delimited extension over a separator-less one in the same file name`() {
    appleContentType("en_strings.stringsdict").assert.isEqualTo("application/xml")
    appleContentType("en_stringsdict.strings").assert.isEqualTo("text/plain")
  }

  @Test
  fun `resolves the last apple extension within a separator-less segment`() {
    appleContentType("en_strings_stringsdict").assert.isEqualTo("application/xml")
    appleContentType("en_stringsdict_strings").assert.isEqualTo("text/plain")
  }

  @Test
  fun `resolves the last apple extension among dot-separated parts`() {
    appleContentType("en.strings.stringsdict").assert.isEqualTo("application/xml")
    appleContentType("en.stringsdict.strings").assert.isEqualTo("text/plain")
  }

  @Test
  fun `resolves apple extension from a directory segment carrying a separator`() {
    appleContentType("en.strings/Localizable").assert.isEqualTo("text/plain")
    appleContentType("Localizable.stringsdict/en").assert.isEqualTo("application/xml")
  }

  private fun appleContentType(exportRelativePath: String) = getAppleExportPathContentType(exportRelativePath)
}
