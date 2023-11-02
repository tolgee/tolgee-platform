package io.tolgee.api.v2.controllers.automations

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.AutomationsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.service.automations.AutomationService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AutomationsControllerTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: AutomationsTestData

  @Autowired
  lateinit var automationsService: AutomationService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = AutomationsTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = listOf(Feature.PROJECT_LEVEL_CDN_STORAGES)
  }

  @AfterEach
  fun after() {
    resetServerProperties()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates automation`() {
    performProjectAuthPost(
      "automations",
      mapOf(
        "name" to "Publish",
        "triggers" to listOf(
          mapOf("type" to "TRANSLATION_DATA_MODIFICATION")
        ),
        "actions" to listOf(
          mapOf(
            "type" to "CDN_PUBLISH",
            "cdnExporterId" to testData.s3Exporter.self.id
          )
        )
      )
    ).andIsOk.andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo("Publish")
      node("actions") {
        isArray.hasSize(1)
        node("[0]") {
          node("type").isEqualTo("CDN_PUBLISH")
          node("cdnExporter") {
            node("id").isValidId
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates automation`() {
    performProjectAuthPut(
      "automations/${testData.automation.self.id}",
      mapOf(
        "name" to "Azure Export",
        "triggers" to listOf(
          mapOf("type" to "TRANSLATION_DATA_MODIFICATION")
        ),
        "actions" to listOf(
          mapOf(
            "type" to "CDN_PUBLISH",
            "cdnExporterId" to testData.azureExporter.self.id
          )
        )
      )
    ).andIsOk.andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo("Azure Export")
      node("actions") {
        isArray.hasSize(1)
        node("[0]") {
          node("type").isEqualTo("CDN_PUBLISH")
          node("cdnExporter") {
            node("id").isEqualTo(testData.azureExporter.self.id)
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists automations`() {
    performProjectAuthGet("automations").andIsOk.andAssertThatJson {
      node("_embedded.automations") {
        isArray.hasSize(1)
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get single`() {
    performProjectAuthGet("automations/${testData.automation.self.id}").andIsOk
      .andAssertThatJson {
        node("name").isEqualTo("Default")
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes automation`() {
    performProjectAuthDelete(
      "automations/${testData.automation.self.id}"
    ).andIsOk
    automationsService.find(testData.s3Exporter.self.id).assert.isNull()
  }

  private fun resetServerProperties() {
    tolgeeProperties.cdn.s3.clear()
    tolgeeProperties.cdn.azure.clear()
  }
}
