package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ResolvableImportTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
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
class KeyControllerResolvableImportTest : ProjectAuthControllerTest("/v2/projects/") {

  val testData = ResolvableImportTestData()
  var uploadedImageId by Delegates.notNull<Long>()

  @BeforeEach
  fun setup() {
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
    uploadedImageId = imageUploadService.store(generateImage(), userAccount!!).id
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports`() {
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to listOf(
          mapOf(
            "name" to "key-1",
            "namespace" to "namespace-1",
            "translations" to mapOf(
              "de" to mapOf(
                "text" to "changed",
                "resolution" to "OVERRIDE"
              ),
              "en" to mapOf(
                "text" to "new",
                "resolution" to "NEW"
              )
            ),
            "screenshots" to listOf(
              mapOf(
                "text" to "Oh oh Oh",
                "uploadedImageId" to uploadedImageId,
                "positions" to listOf(
                  mapOf(
                    "x" to 100,
                    "y" to 150,
                    "width" to 80,
                    "height" to 100
                  ),
                  mapOf(
                    "x" to 500,
                    "y" to 200,
                    "width" to 30,
                    "height" to 20
                  )
                )
              )
            )
          ),
          mapOf(
            "name" to "key-2",
            "namespace" to "namespace-1",
            "translations" to mapOf(
              "en" to mapOf(
                "text" to "new",
                "resolution" to "KEEP"
              )
            ),
            "removeScreenshotIds" to listOf(testData.key2Screenshot.id, testData.key1and2Screenshot.id),
            "screenshots" to listOf(
              mapOf(
                "text" to "Oh oh Oh",
                "uploadedImageId" to uploadedImageId,
                "positions" to listOf(
                  mapOf(
                    "x" to 100,
                    "y" to 150,
                    "width" to 80,
                    "height" to 100
                  )
                )
              )
            )
          ),
        )
      )
    ).andIsOk.andAssertThatJson {
      node("keys").isArray.hasSize(2)
      node("screenshots") {
        node(uploadedImageId.toString()) {
          node("id").isNumber
          node("filename").isString
        }
      }
    }

    screenshotService.findByIdIn(listOf(testData.key2Screenshot.id, testData.key1and2Screenshot.id))
      .assert.hasSize(1) // one is deleted

    executeInNewTransaction {
      assertTranslationText("namespace-1", "key-1", "de", "changed")
      assertTranslationText("namespace-1", "key-1", "en", "new")
      assertTranslationText("namespace-1", "key-2", "en", "existing translation")
    }
  }

  fun assertTranslationText(namespace: String?, keyName: String, languageTag: String, expectedText: String) {
    projectService.get(testData.projectBuilder.self.id)
      .keys
      .find { it.name == keyName && it.namespace?.name == namespace }!!
      .translations
      .find { it.language.tag == languageTag }!!
      .text
      .assert.isEqualTo(
        expectedText
      )
  }
}
