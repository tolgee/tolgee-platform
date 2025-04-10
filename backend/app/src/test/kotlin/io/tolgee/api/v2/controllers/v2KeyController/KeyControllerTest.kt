package io.tolgee.api.v2.controllers.v2KeyController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.fixtures.andAssertError
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
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
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns all keys`() {
    testData.addNKeys(120)
    saveTestDataAndPrepare()
    performProjectAuthGet("keys")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(20)
          node("[0].id").isValidId
          node("[1].name").isEqualTo("second_key")
          node("[2].namespace").isEqualTo("null")
          node("[1].description").isEqualTo("description")
        }
      }
    performProjectAuthGet("keys?page=1")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(20)
          node("[0].id").isValidId
          node("[1].name").isEqualTo("key_19")
          node("[2].namespace").isEqualTo("null")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns single key`() {
    saveTestDataAndPrepare()
    val keyId = testData.keyWithReferences.id
    performProjectAuthGet("keys/$keyId")
      .andIsOk
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("key_with_referecnces")
        node("namespace").isNull()
        node("description").isNull()
        node("custom").isObject.containsKeys("custom")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when not valid`() {
    saveTestDataAndPrepare()

    performProjectAuthPost("keys", CreateKeyDto(name = ""))
      .andIsBadRequest.andPrettyPrint
      .andAssertThatJson {
        node("STANDARD_VALIDATION") {
          node("name").isString
        }
      }

    performProjectAuthPost("keys", CreateKeyDto(name = LONGER_NAME))
      .andIsBadRequest.andPrettyPrint
      .andAssertThatJson {
        node("STANDARD_VALIDATION") {
          node("name").isEqualTo("length must be between 1 and 2000")
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not create key when key exists`() {
    saveTestDataAndPrepare()

    performProjectAuthPost("keys", CreateKeyDto(name = "first_key"))
      .andIsBadRequest
      .andAssertError
      .isCustomValidation
      .hasMessage("key_exists")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates key name`() {
    saveTestDataAndPrepare()

    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = "test"))
      .andIsOk.andPrettyPrint
      .andAssertThatJson {
        node("name").isEqualTo("test")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates key description`() {
    saveTestDataAndPrepare()

    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = "name", description = "desc"))
      .andIsOk.andPrettyPrint
      .andAssertThatJson {
        node("description").isEqualTo("desc")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update if invalid`() {
    saveTestDataAndPrepare()

    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = ""))
      .andIsBadRequest
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = LONGER_NAME))
      .andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not update if from other project`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project2 }
    performProjectAuthPut("keys/${testData.firstKey.id}", EditKeyDto(name = "aasda"))
      .andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes single key`() {
    saveTestDataAndPrepare()

    performProjectAuthDelete("keys/${testData.firstKey.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.firstKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes single key with references`() {
    saveTestDataAndPrepare()

    performProjectAuthDelete("keys/${testData.keyWithReferences.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.keyWithReferences.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `doesn't delete screenshot when another reference exists`() {
    saveTestDataAndPrepare()

    performProjectAuthDelete("keys/${testData.keyWithReferences.id}", null).andIsOk
    screenshotService.find(testData.screenshot.id).assert.isNotNull
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys with references`() {
    saveTestDataAndPrepare()

    performProjectAuthDelete("keys/${testData.keyWithReferences.id},${testData.keyWithReferences.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.keyWithReferences.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not delete if not in project`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project2 }
    performProjectAuthDelete("keys/${testData.firstKey.id}", null)
      .andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project }
    performProjectAuthDelete("keys/${testData.firstKey.id},${testData.secondKey.id}", null).andIsOk
    assertThat(keyService.findOptional(testData.firstKey.id)).isEmpty
    assertThat(keyService.findOptional(testData.secondKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `deletes multiple keys via post`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project }
    performProjectAuthDelete(
      "keys",
      mapOf("ids" to listOf(testData.firstKey.id, testData.secondKey.id)),
    ).andIsOk
    assertThat(keyService.findOptional(testData.firstKey.id)).isEmpty
    assertThat(keyService.findOptional(testData.secondKey.id)).isEmpty
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `does not delete multiple if not in project`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project2 }
    performProjectAuthDelete("keys/${testData.secondKey.id},${testData.firstKey.id}", null)
      .andIsBadRequest
      .andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `imports keys`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project }
    performProjectAuthPost(
      "keys/import",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "first_key",
              "translations" to
                mapOf("en" to "hello"),
              "description" to "description",
              "tags" to listOf("tag1", "tag2"),
            ),
            mapOf(
              "name" to "new_key",
              "description" to "description",
              "translations" to
                mapOf("en" to "hello"),
              "tags" to listOf("tag1", "tag2", "test"),
            ),
            mapOf(
              "name" to "key_without_tags",
              "translations" to
                mapOf("en" to "hello"),
            ),
            mapOf(
              "name" to "key_with_empty_tags",
              "translations" to
                mapOf("en" to "hello"),
              "tags" to listOf<String>(),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      val firstKey = keyService.get(testData.firstKey.id)
      firstKey.translations
        .find { it.language.tag == "en" }
        .assert
        .isNull()
      firstKey.keyMeta
        ?.description.assert
        .isNull()

      val key =
        projectService.get(testData.project.id).keys.find {
          it.name == "new_key"
        }
      key!!
        .keyMeta!!
        .description.assert
        .isEqualTo("description")

      key.assert.isNotNull()
      key.keyMeta!!
        .tags.assert
        .hasSize(3)
      key.translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("hello")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `imports single key with no tags`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project }
    performProjectAuthPost(
      "keys/import",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "first_key",
              "translations" to
                mapOf("en" to "hello"),
            ),
          ),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `imports keys with same tags`() {
    saveTestDataAndPrepare()

    projectSupplier = { testData.project }
    performProjectAuthPost(
      "keys/import",
      jacksonObjectMapper().readValue(
        """
        {
        	"keys": [
        		{
        			"name": "FavoriteMapTooltip",
        			"namespace": "QuickActions",
        			"translations":
        			{
        				"en": "Path: {0}"
        			},
        			"tags": [
        				"OriginalText:Path: {0}",
        				"OriginalHash:3103973689"
        			]
        		},
        		{
        			"name": "RecentMapTooltip",
        			"namespace": "QuickActions",
        			"translations":
        			{
        				"en": "Path: {0}"
        			},
        			"tags": [
        				"OriginalText:Path: {0}",
        				"OriginalHash:3103973689"
        			]
        		}
        	]
        }
        """.trimIndent(),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `new translations are not outdated`() {
    saveTestDataAndPrepare()
    performProjectAuthPost(
      "keys",
      CreateKeyDto(name = "super_key", translations = mapOf("en" to "Hello", "de" to "Hallo")),
    ).andIsCreated

    executeInNewTransaction {
      keyService.find(testData.project.id, "super_key", null)!!.translations.forEach {
        it.outdated.assert.isFalse()
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `import keys checks permissions`() {
    val testData = LanguagePermissionsTestData()
    testDataService.saveTestData(testData.root)

    projectSupplier = { testData.project }
    userAccount = testData.viewEnOnlyUser
    performProjectAuthPost(
      "keys/import",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "first_key",
              "translations" to
                mapOf("de" to "hello"),
            ),
          ),
      ),
    ).andIsForbidden
  }

  private fun saveTestDataAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }
}
