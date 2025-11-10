package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysInfoTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import kotlin.properties.Delegates

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerInfoTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeysInfoTestData
  var uploadedImageId by Delegates.notNull<Long>()

  @BeforeEach
  fun setup() {
    executeInNewTransaction {
      testData = KeysInfoTestData()
      testDataService.saveTestData(testData.root)
      projectSupplier = { testData.projectBuilder.self }
      userAccount = testData.user
      uploadedImageId = imageUploadService.store(generateImage(), userAccount!!, null).id
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns the data`() {
    executeInNewTransaction {
      val keys =
        (1..20)
          .map {
            mapOf("name" to "key-$it")
          } +
          listOf(
            mapOf("name" to "key-22", "namespace" to "ns"),
            mapOf("name" to "key-1", "namespace" to "namespace-1"),
            mapOf("name" to "key-300"),
          )

      performProjectAuthPost(
        "keys/info",
        mapOf("keys" to keys, "languageTags" to listOf("de")),
      ).andIsOk
        .andAssertThatJson {
          node("_embedded.keys") {
            isArray.hasSize(22)
            node("[0]") {
              node("custom") {
                isObject.hasSize(1)
                node("key").isEqualTo("value")
              }
            }
            node("[20]") {
              node("namespace").isEqualTo("namespace-1")
              node("name").isEqualTo("key-1")
              node("description").isEqualTo("description")
              node("screenshots") {
                isArray
                node("[0]") {
                  node("keyReferences").isArray.hasSize(2)
                }
              }
              node("translations") {
                node("de") {
                  node("text").isEqualTo("existing translation")
                }
                node("en").isAbsent()
              }
            }
          }
        }.andPrettyPrint
    }
  }
}
