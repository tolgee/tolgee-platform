package io.tolgee.api.v2.controllers.machineTranslation

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class MachineTranslationProvidersControllerTest : AbstractControllerTest() {
  @BeforeEach
  fun setup() {
    mockProperties()
  }

  private fun mockProperties() {
    awsMachineTranslationProperties.defaultEnabled = true
    awsMachineTranslationProperties.defaultPrimary = false
    awsMachineTranslationProperties.accessKey = "dummy"
    awsMachineTranslationProperties.secretKey = "dummy"
    googleMachineTranslationProperties.defaultEnabled = true
    googleMachineTranslationProperties.defaultPrimary = true
    googleMachineTranslationProperties.apiKey = "dummy"
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns the info`() {
    performGet("/v2/public/machine-translation-providers").andIsOk.andPrettyPrint.andAssertThatJson {
      node("GOOGLE.supportedLanguages").isArray.hasSizeGreaterThan(10)
      node("AWS.supportedLanguages").isArray.hasSizeGreaterThan(10)
    }
  }
}
