package io.tolgee.api.v2.controllers.v2ImportController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ResolvableImportTestData
import io.tolgee.dtos.request.ImageUploadInfoDto
import io.tolgee.dtos.request.KeyInScreenshotPositionDto
import io.tolgee.dtos.request.importKeysResolvable.ResolvableTranslationResolution
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableItemRequest
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableRequest
import io.tolgee.dtos.request.importKeysResolvable.SingleStepImportResolvableTranslationRequest
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.key.Key
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import kotlin.properties.Delegates

@SpringBootTest
@AutoConfigureMockMvc
class SingleStepImportControllerResolvableTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ResolvableImportTestData

  var uploadedImageId by Delegates.notNull<Long>()

  @BeforeEach
  fun setup() {
    testData = ResolvableImportTestData()
    testData.projectBuilder.self.translationProtection = TranslationProtection.PROTECT_REVIEWED
    testData.projectBuilder.self.useNamespaces = true
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
    uploadedImageId =
      imageUploadService
        .store(
          generateImage(),
          userAccount!!,
          ImageUploadInfoDto(location = "My cool frame"),
        ).id
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports screenshots`() {
    val request =
      SingleStepImportResolvableRequest(
        keys =
          listOf(
            SingleStepImportResolvableItemRequest(
              name = "key-1",
              namespace = "namespace-1",
              translations =
                mapOf(
                  "de" to
                    SingleStepImportResolvableTranslationRequest(
                      text = "changed",
                      resolution = ResolvableTranslationResolution.OVERRIDE,
                    ),
                  "en" to
                    SingleStepImportResolvableTranslationRequest(
                      text = "new",
                      resolution = ResolvableTranslationResolution.EXPECT_NO_CONFLICT,
                    ),
                ),
              screenshots =
                listOf(
                  KeyScreenshotDto(
                    text = "Oh oh Oh",
                    uploadedImageId = uploadedImageId,
                    positions =
                      listOf(
                        KeyInScreenshotPositionDto(
                          x = 100,
                          y = 150,
                          width = 80,
                          height = 100,
                        ),
                        KeyInScreenshotPositionDto(
                          x = 500,
                          y = 200,
                          width = 30,
                          height = 20,
                        ),
                      ),
                  ),
                ),
            ),
            SingleStepImportResolvableItemRequest(
              name = "key-2",
              namespace = "namespace-1",
              screenshots =
                listOf(
                  KeyScreenshotDto(
                    text = "Oh oh Oh",
                    uploadedImageId = uploadedImageId,
                    positions =
                      listOf(
                        KeyInScreenshotPositionDto(
                          x = 100,
                          y = 150,
                          width = 80,
                          height = 100,
                        ),
                      ),
                  ),
                ),
            ),
            SingleStepImportResolvableItemRequest(
              name = "nonexisting",
              namespace = "namespace-1",
              screenshots =
                listOf(
                  KeyScreenshotDto(
                    uploadedImageId = uploadedImageId,
                    positions =
                      listOf(
                        KeyInScreenshotPositionDto(
                          x = 100,
                          y = 150,
                          width = 80,
                          height = 100,
                        ),
                      ),
                  ),
                ),
            ),
          ),
      )

    performProjectAuthPost(
      "single-step-import-resolvable",
      request,
    ).andIsOk

    executeInNewTransaction {
      screenshotService.findByIdIn(listOf(testData.key2Screenshot.id)).assert.isNotEmpty
      screenshotService.findByIdIn(listOf(testData.key1and2Screenshot.id)).assert.isEmpty()

      assertTranslationText("namespace-1", "key-1", "de", "changed")
      assertTranslationText("namespace-1", "key-1", "en", "new")
      assertTranslationText("namespace-1", "key-2", "en", "existing translation")

      getKey("namespace-1", "key-1")?.keyScreenshotReferences.assert.hasSize(1)
      getKey("namespace-1", "key-2")?.keyScreenshotReferences.assert.hasSize(2)
      getKey("namespace-1", "nonexisting")?.keyScreenshotReferences.assert.hasSize(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports unreviewed or new translations`() {
    val request =
      SingleStepImportResolvableRequest(
        keys =
          listOf(
            SingleStepImportResolvableItemRequest(
              name = "key-1",
              namespace = "namespace-1",
              translations =
                mapOf(
                  "de" to
                    SingleStepImportResolvableTranslationRequest(
                      text = "changed",
                      resolution = ResolvableTranslationResolution.OVERRIDE,
                    ),
                  "en" to
                    SingleStepImportResolvableTranslationRequest(
                      text = "new",
                      resolution = ResolvableTranslationResolution.EXPECT_NO_CONFLICT,
                    ),
                ),
            ),
          ),
      )
    performProjectAuthPost(
      "single-step-import-resolvable",
      request,
    ).andIsOk.andAssertThatJson {
      node("unresolvedConflicts").isNull()
    }

    executeInNewTransaction {
      assertTranslationText("namespace-1", "key-1", "de", "changed")
      assertTranslationText("namespace-1", "key-1", "en", "new")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `fails on expect_no_conflict`() {
    val request =
      SingleStepImportResolvableRequest(
        keys =
          listOf(
            SingleStepImportResolvableItemRequest(
              name = "key-1",
              namespace = "namespace-1",
              translations =
                mapOf(
                  "de" to
                    SingleStepImportResolvableTranslationRequest(
                      text = "new",
                      resolution = ResolvableTranslationResolution.EXPECT_NO_CONFLICT,
                    ),
                ),
            ),
          ),
      )
    performProjectAuthPost(
      "single-step-import-resolvable",
      request,
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("expect_no_conflict_failed")
      node("params[0].name").isEqualTo("key-1")
      node("params[0].namespace").isEqualTo("namespace-1")
    }

    executeInNewTransaction {
      assertTranslationText("namespace-1", "key-1", "de", "existing translation")
    }
  }

  private fun getKey(
    namespace: String?,
    keyName: String,
  ): Key? {
    return projectService.get(testData.projectBuilder.self.id).keys.find {
      it.name == keyName &&
        it.namespace?.name == namespace
    }
  }

  private fun assertTranslationText(
    namespace: String?,
    keyName: String,
    languageTag: String,
    expectedText: String,
  ) {
    val text =
      getKey(namespace, keyName)?.translations?.find { it.language.tag == languageTag }?.text

    text.assert.isEqualTo(
      expectedText,
    )
  }
}
