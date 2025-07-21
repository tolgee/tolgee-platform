package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ResolvableImportTestData
import io.tolgee.dtos.request.ImageUploadInfoDto
import io.tolgee.fixtures.*
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import kotlin.properties.Delegates

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerResolvableImportProtectedTranslationsTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ResolvableImportTestData
  var uploadedImageId by Delegates.notNull<Long>()
  var uploadedImageId2 by Delegates.notNull<Long>()

  @Value("classpath:keyImportRequest.json")
  lateinit var realData: Resource

  @BeforeEach
  fun setup() {
    testData = ResolvableImportTestData()
    testData.projectBuilder.self.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.translatorUser
    uploadedImageId =
      imageUploadService
        .store(
          generateImage(),
          userAccount!!,
          ImageUploadInfoDto(location = "My cool frame"),
        ).id
    uploadedImageId2 =
      imageUploadService
        .store(
          generateImage(),
          testData.viewOnlyUser,
          ImageUploadInfoDto(location = "My cool frame"),
        ).id
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports unreviewed or new translations`() {
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "de" to
                    mapOf(
                      "text" to "changed",
                      "resolution" to "OVERRIDE",
                    ),
                  "en" to
                    mapOf(
                      "text" to "new",
                      "resolution" to "NEW",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("keys").isArray.hasSize(1)
    }

    executeInNewTransaction {
      assertTranslationText("namespace-1", "key-1", "de", "changed")
      assertTranslationText("namespace-1", "key-1", "en", "new")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it fails to import reviewed translation`() {
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "keyWith2Translations",
              "translations" to
                mapOf(
                  "de" to
                    mapOf(
                      "text" to "changed",
                      "resolution" to "OVERRIDE",
                    ),
                  "en" to
                    mapOf(
                      "text" to "changed",
                      "resolution" to "OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("keys").isArray.hasSize(1)
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
    projectService
      .get(testData.projectBuilder.self.id)
      .keys
      .find { it.name == keyName && it.namespace?.name == namespace }!!
      .translations
      .find { it.language.tag == languageTag }!!
      .text
      .assert
      .isEqualTo(
        expectedText,
      )
  }
}
