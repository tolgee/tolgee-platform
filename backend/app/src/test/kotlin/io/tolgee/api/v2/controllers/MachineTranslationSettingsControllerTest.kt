package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.dtos.request.MachineTranslationLanguagePropsDto
import io.tolgee.dtos.request.SetMachineTranslationSettingsDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.mtServiceConfig.Formality
import io.tolgee.service.machineTranslation.MtServiceInfo
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class MachineTranslationSettingsControllerTest : ProjectAuthControllerTest() {

  lateinit var testData: MtSettingsTestData

  @BeforeEach
  fun setup() {
    mockProperties()
    initTestData()
  }

  private fun initTestData() {
    testData = MtSettingsTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
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
  fun `it sets the configuration`() {
    performAuthPut(
      "/v2/projects/${project.id}/machine-translation-service-settings",
      SetMachineTranslationSettingsDto(
        listOf(
          MachineTranslationLanguagePropsDto(
            targetLanguageId = null,
            primaryService = MtServiceType.GOOGLE,
            enabledServices = setOf(MtServiceType.AWS, MtServiceType.GOOGLE)
          ),
          MachineTranslationLanguagePropsDto(
            targetLanguageId = testData.englishLanguage.id,
            primaryService = MtServiceType.GOOGLE,
            enabledServicesInfo = setOf(
              MtServiceInfo(MtServiceType.GOOGLE, null),
              MtServiceInfo(MtServiceType.AWS, Formality.DEFAULT)
            )
          )
        )
      )
    ).andIsOk.andAssertThatJson {
      node("_embedded.languageConfigs") {
        node("[0]") {
          node("targetLanguageId").isNull()
          node("primaryService").isEqualTo("GOOGLE")
          node("enabledServices").isArray.isEqualTo("""[ "GOOGLE", "AWS" ]""")
          node("enabledServicesInfo") {
            isArray
            node("[0]") {
              node("serviceType").isEqualTo("GOOGLE")
              node("formality").isNull()
            }
            node("[1]") {
              node("serviceType").isEqualTo("AWS")
              node("formality").isEqualTo("DEFAULT")
            }
          }
        }
        node("[1]") {
          node("targetLanguageId").isValidId
          node("targetLanguageTag").isEqualTo("en")
          node("targetLanguageName").isEqualTo("English")
          node("primaryService").isEqualTo("GOOGLE")
          node("enabledServices").isArray.isEqualTo("""[ "GOOGLE", "AWS" ]""")
          node("enabledServicesInfo") {
            isArray
            node("[0]") {
              node("serviceType").isEqualTo("GOOGLE")
              node("formality").isNull()
            }
            node("[1]") {
              node("serviceType").isEqualTo("AWS")
              node("formality").isEqualTo("DEFAULT")
            }
          }
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it removes service from the configuration`() {
    performAuthPut(
      "/v2/projects/${project.id}/machine-translation-service-settings",
      SetMachineTranslationSettingsDto(
        listOf(
          MachineTranslationLanguagePropsDto(
            targetLanguageId = testData.spanishLanguage.id,
            primaryService = MtServiceType.GOOGLE,
            enabledServices = setOf(MtServiceType.AWS)
          )
        )
      )
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns the configuration`() {
    performAuthGet(
      "/v2/projects/${project.id}/machine-translation-service-settings"
    ).andPrettyPrint.andAssertThatJson {
      node("_embedded.languageConfigs") {
        node("[0]") {
          node("targetLanguageId").isNull()
          node("primaryService").isEqualTo("AWS")
          node("enabledServices").isArray.isEqualTo("""[ "GOOGLE", "AWS" ]""")
        }
        node("[1]") {
          node("targetLanguageId").isValidId
          node("targetLanguageTag").isEqualTo("de")
          node("targetLanguageName").isEqualTo("German")
          node("primaryService").isEqualTo("AWS")
          node("enabledServices").isArray.isEqualTo("""[ "AWS" ]""")
        }
      }
    }
  }
}
