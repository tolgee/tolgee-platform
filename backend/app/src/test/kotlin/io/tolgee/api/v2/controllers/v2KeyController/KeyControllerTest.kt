package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  companion object {
    val MAX_OK_NAME = (1..2000).joinToString("") { "a" }
    val LONGER_NAME = (1..2001).joinToString("") { "a" }
  }

  lateinit var testData: KeysTestData

  @BeforeEach
  fun setup() {
    testData = KeysTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when not valid`() {
    performProjectAuthPost("keys", CreateKeyDto(name = ""))
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("STANDARD_VALIDATION") {
          node("name").isString
        }
      }

    performProjectAuthPost("keys", CreateKeyDto(name = LONGER_NAME))
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("STANDARD_VALIDATION") {
          node("name").isEqualTo("length must be between 1 and 2000")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when key exists`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "first_key"))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation.hasMessage("key_exists")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates key name`() {
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = "test"))
      .andIsOk.andPrettyPrint.andAssertThatJson {
        node("name").isEqualTo("test")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update if invalid`() {
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = ""))
      .andIsBadRequest
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = LONGER_NAME))
      .andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update if from other project`() {
    projectSupplier = { testData.project2 }
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = "aasda"))
      .andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes single key`() {
    performProjectAuthDelete("keys/${testData.firstKey.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.firstKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes single key with references`() {
    performProjectAuthDelete("keys/${testData.keyWithReferences.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.keyWithReferences.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys with references`() {
    performProjectAuthDelete("keys/${testData.keyWithReferences.id},${testData.keyWithReferences.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.keyWithReferences.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not delete if not in project`() {
    projectSupplier = { testData.project2 }
    performProjectAuthDelete("keys/${testData.firstKey.id}", null)
      .andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys`() {
    projectSupplier = { testData.project }
    performProjectAuthDelete("keys/${testData.firstKey.id},${testData.secondKey.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.firstKey.id)).isEmpty
    assertThat(keyService.findOptional(testData.secondKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys via post`() {
    projectSupplier = { testData.project }
    performProjectAuthDelete(
      "keys", mapOf("ids" to listOf(testData.firstKey.id, testData.secondKey.id))
    ).andIsOk
    assertThat(keyService.findOptional(testData.firstKey.id)).isEmpty
    assertThat(keyService.findOptional(testData.secondKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not delete multiple if not in project`() {
    projectSupplier = { testData.project2 }
    performProjectAuthDelete("keys/${testData.secondKey.id},${testData.firstKey.id}", null)
      .andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `imports keys`() {
    projectSupplier = { testData.project }
    performProjectAuthPost(
      "keys/import",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "first_key",
              "translations" to
                mapOf("en" to "hello")
            ),
            mapOf(
              "name" to "new_key",
              "translations" to
                mapOf("en" to "hello")
            )
          )
      )
    ).andIsOk

    executeInNewTransaction {
      keyService.get(testData.firstKey.id).translations.find { it.language.tag == "en" }.assert.isNull()
      val key = projectService.get(testData.project.id).keys.find {
        it.name == "new_key"
      }
      key.assert.isNotNull()
      key!!.translations.find { it.language.tag == "en" }!!.text.assert.isEqualTo("hello")
    }
  }
}
