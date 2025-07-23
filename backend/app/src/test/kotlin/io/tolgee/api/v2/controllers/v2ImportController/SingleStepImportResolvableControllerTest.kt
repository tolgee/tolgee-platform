package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ResolvableImportTestData
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableItemRequest
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableRequest
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableTranslationRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.service.dataImport.ForceMode
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource

@SpringBootTest
@AutoConfigureMockMvc
class SingleStepImportResolvableControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ResolvableImportTestData

  @Value("classpath:keyImportRequest.json")
  lateinit var realData: Resource

  @BeforeEach
  fun setup() {
    testData = ResolvableImportTestData()
    testData.projectBuilder.self.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.projectBuilder.self.useNamespaces = true
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports unreviewed or new translations`() {
    val request = SingleStepImportResolvableRequest(
      keys = listOf(
        SingleStepImportResolvableItemRequest(
          name = "key-1",
          namespace = "namespace-1",
          translations = mapOf(
            "de" to SingleStepImportResolvableTranslationRequest(
              text = "changed",
              forceMode = ForceMode.OVERRIDE,
            ),
            "en" to SingleStepImportResolvableTranslationRequest(
              text = "new",
              forceMode = ForceMode.NO_FORCE,
            )
          )
        )
      )
    )
    performProjectAuthPost(
      "single-step-import-resolvable",
      request
    ).andIsOk.andAssertThatJson {
      node("failedKeys").isNull()
    }

    executeInNewTransaction {
      assertTranslationText("namespace-1", "key-1", "de", "changed")
      assertTranslationText("namespace-1", "key-1", "en", "new")
    }
  }

  fun assertTranslationText(
    namespace: String?,
    keyName: String,
    languageTag: String,
    expectedText: String,
  ) {
    val text = projectService
      .get(testData.projectBuilder.self.id)
      .keys
      .find { it.name == keyName && it.namespace?.name == namespace }
      ?.translations
      ?.find { it.language.tag == languageTag }
      ?.text

    text
      .assert
      .isEqualTo(
        expectedText,
      )
  }
}
