package io.tolgee.api.v2.controllers.machineTranslation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.dtos.request.MachineTranslationLanguagePropsDto
import io.tolgee.dtos.request.SetMachineTranslationSettingsDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.Language
import io.tolgee.model.mtServiceConfig.Formality
import io.tolgee.service.machineTranslation.MtServiceInfo
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
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
    llmProperties.enabled = true
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
            enabledServices = setOf(MtServiceType.AWS, MtServiceType.GOOGLE),
          ),
          MachineTranslationLanguagePropsDto(
            targetLanguageId = testData.englishLanguage.id,
            primaryService = MtServiceType.GOOGLE,
            enabledServicesInfo =
              setOf(
                MtServiceInfo(MtServiceType.GOOGLE, null),
                MtServiceInfo(MtServiceType.AWS, Formality.DEFAULT),
              ),
          ),
        ),
      ),
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
  fun `it sets AWS formality for german`() {
    performAuthPut(
      "/v2/projects/${project.id}/machine-translation-service-settings",
      SetMachineTranslationSettingsDto(
        listOf(
          MachineTranslationLanguagePropsDto(
            targetLanguageId = null,
            primaryService = MtServiceType.GOOGLE,
            enabledServices = setOf(MtServiceType.AWS, MtServiceType.GOOGLE),
          ),
          MachineTranslationLanguagePropsDto(
            targetLanguageId = testData.germanLanguage.id,
            primaryService = MtServiceType.AWS,
            enabledServicesInfo =
              setOf(
                MtServiceInfo(MtServiceType.GOOGLE, null),
                MtServiceInfo(MtServiceType.AWS, Formality.FORMAL),
              ),
          ),
        ),
      ),
    )

    executeInNewTransaction {
      val germanSetting =
        mtServiceConfigService.getProjectSettings(testData.projectBuilder.self)
          .find { it.targetLanguage?.id == testData.germanLanguage.id }
      germanSetting!!.awsFormality.assert.isEqualTo(Formality.FORMAL)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `formality can be set for default`() {
    performAuthPut(
      "/v2/projects/${project.id}/machine-translation-service-settings",
      SetMachineTranslationSettingsDto(
        listOf(
          MachineTranslationLanguagePropsDto(
            targetLanguageId = null,
            primaryService = MtServiceType.GOOGLE,
            enabledServicesInfo =
              setOf(
                MtServiceInfo(MtServiceType.GOOGLE, null),
                MtServiceInfo(MtServiceType.AWS, Formality.FORMAL),
              ),
          ),
        ),
      ),
    )

    executeInNewTransaction {
      val germanSetting =
        mtServiceConfigService.getProjectSettings(testData.projectBuilder.self)
          .find { it.targetLanguage?.id == null }
      germanSetting!!.awsFormality.assert.isEqualTo(Formality.FORMAL)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it sets primary service via info`() {
    performAuthPut(
      "/v2/projects/${project.id}/machine-translation-service-settings",
      SetMachineTranslationSettingsDto(
        listOf(
          MachineTranslationLanguagePropsDto(
            targetLanguageId = testData.germanLanguage.id,
            primaryService = MtServiceType.PROMPT,
            primaryServiceInfo = MtServiceInfo(MtServiceType.PROMPT, Formality.FORMAL, promptId = null),
          ),
        ),
      ),
    ).andAssertThatJson {
      node("_embedded.languageConfigs") {
        node("[1]") {
          node("primaryServiceInfo.formality").isEqualTo("FORMAL")
        }
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it validates the config`() {
    performSet(null, MtServiceType.AWS, Formality.FORMAL).andIsOk
    performSet(testData.englishLanguage, MtServiceType.AWS, Formality.FORMAL).andIsBadRequest
    performSet(testData.germanLanguage, MtServiceType.AWS, Formality.FORMAL).andIsOk
    performSet(testData.germanLanguage, MtServiceType.AWS, Formality.DEFAULT).andIsOk
    performSet(testData.germanLanguage, MtServiceType.PROMPT, Formality.FORMAL).andIsOk
    performSet(testData.englishLanguage, MtServiceType.PROMPT, Formality.FORMAL).andIsOk
    performSet(
      MachineTranslationLanguagePropsDto(
        testData.englishLanguage.id,
        primaryServiceInfo = MtServiceInfo(MtServiceType.PROMPT, Formality.FORMAL),
      ),
    ).andIsOk
  }

  private fun performSet(
    language: Language?,
    mtServiceType: MtServiceType,
    formality: Formality,
  ) = performAuthPut(
    "/v2/projects/${project.id}/machine-translation-service-settings",
    SetMachineTranslationSettingsDto(
      listOf(
        MachineTranslationLanguagePropsDto(
          targetLanguageId = language?.id,
          primaryServiceInfo = MtServiceInfo(mtServiceType, Formality.DEFAULT),
          enabledServicesInfo =
            setOf(
              MtServiceInfo(mtServiceType, formality),
            ),
        ),
      ),
    ),
  )

  private fun performSet(props: MachineTranslationLanguagePropsDto) =
    performAuthPut(
      "/v2/projects/${project.id}/machine-translation-service-settings",
      SetMachineTranslationSettingsDto(
        listOf(
          props,
        ),
      ),
    )

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
            enabledServices = setOf(MtServiceType.AWS),
          ),
        ),
      ),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns the configuration`() {
    performAuthGet(
      "/v2/projects/${project.id}/machine-translation-service-settings",
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

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns the info`() {
    performAuthGet(
      "/v2/projects/${project.id}/machine-translation-language-info",
    ).andPrettyPrint.andAssertThatJson {
      node("_embedded.languageInfos") {
        isArray
        node("[0]") {
          node("languageTag").isEqualTo(null)
          node("supportedServices") {
            isArray
            node("[0]") {
              node("serviceType").isEqualTo("GOOGLE")
              node("formalitySupported").isEqualTo(false)
            }
            node("[1]") {
              node("serviceType").isEqualTo("AWS")
              node("formalitySupported").isEqualTo(true)
            }
          }
        }
        node("[2]") {
          node("languageTag").isEqualTo("de")
          node("supportedServices") {
            isArray
            node("[0]") {
              node("serviceType").isEqualTo("GOOGLE")
              node("formalitySupported").isEqualTo(false)
            }
            node("[1]") {
              node("serviceType").isEqualTo("AWS")
              node("formalitySupported").isEqualTo(true)
            }
          }
        }
      }
    }
  }
}
