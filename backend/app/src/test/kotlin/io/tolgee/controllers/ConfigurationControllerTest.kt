package io.tolgee.controllers

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ConfigurationControllerTest : AbstractControllerTest() {
  @BeforeEach
  fun setup() {
    initProperties()
  }

  private fun initProperties() {
    machineTranslationProperties.freeCreditsAmount = 1000
    awsMachineTranslationProperties.accessKey = "dummy"
    awsMachineTranslationProperties.defaultEnabled = false
    awsMachineTranslationProperties.secretKey = "dummy"
    googleMachineTranslationProperties.apiKey = "dummy"
    googleMachineTranslationProperties.defaultEnabled = true
    awsMachineTranslationProperties.defaultPrimary = true
    googleMachineTranslationProperties.defaultPrimary = false
  }

  @Test
  fun `returns correct services public config`() {
    performGet("/api/public/configuration").andIsOk.andAssertThatJson {
      node("machineTranslationServices") {
        node("services") {
          node("AWS") {
            node("enabled").isEqualTo(true)
            node("defaultEnabledForProject").isEqualTo(false)
          }
          node("GOOGLE") {
            node("enabled").isEqualTo(true)
            node("defaultEnabledForProject").isEqualTo(true)
          }
        }
      }
    }
  }
}
