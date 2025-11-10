package io.tolgee.api.v2.controllers

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.Test

class ExportInfoControllerTest : AbstractControllerTest() {
  @Test
  fun `returns formats`() {
    performGet("/v2/public/export-info/formats")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.exportFormats") {
          isArray.hasSizeGreaterThan(5)
          node("[0]") {
            node("extension").isEqualTo("json")
            node("mediaType").isEqualTo("application/json")
            node("defaultFileStructureTemplate")
              .isString
              .isEqualTo("{namespace}/{languageTag}.{extension}")
          }
          node("[1]") {
            node("extension").isEqualTo("json")
            node("mediaType").isEqualTo("application/json")
            node("defaultFileStructureTemplate")
              .isString
              .isEqualTo("{namespace}/{languageTag}.{extension}")
          }
          node("[4]") {
            node("extension").isEqualTo("")
            node("mediaType").isEqualTo("")
            node("defaultFileStructureTemplate")
              .isString
              .isEqualTo("{namespace}/{languageTag}.lproj/Localizable.{extension}")
          }
        }
      }
  }
}
