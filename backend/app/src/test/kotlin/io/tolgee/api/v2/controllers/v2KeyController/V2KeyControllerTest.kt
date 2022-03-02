package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.exceptions.FileStoreException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.ApiScope
import io.tolgee.service.ImageUploadService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class V2KeyControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  companion object {
    val MAX_OK_NAME = (1..2000).joinToString("") { "a" }
    val LONGER_NAME = (1..2001).joinToString("") { "a" }
  }

  @Value("classpath:screenshot.png")
  lateinit var screenshotFile: Resource

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
  fun `creates key`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key"))
      .andIsCreated.andPrettyPrint.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("super_key")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with size 2000`() {
    performProjectAuthPost("keys", CreateKeyDto(name = MAX_OK_NAME))
      .andIsCreated.andPrettyPrint.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo(MAX_OK_NAME)
      }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.KEYS_EDIT])
  @Test
  fun `creates key with keys edit scope`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", translations = mapOf("en" to "", "de" to "")))
      .andIsCreated.andPrettyPrint.andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("super_key")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with translations and tags and screenshots`() {
    val keyName = "super_key"

    val screenshotImages = (1..3).map { imageUploadService.store(screenshotFile, userAccount!!) }
    val screenshotImageIds = screenshotImages.map { it.id }
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        tags = listOf("tag", "tag2"),
        screenshotUploadedImageIds = screenshotImageIds
      )
    ).andIsCreated.andPrettyPrint.andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo(keyName)
      node("tags") {
        isArray.hasSize(2)
        node("[0]") {
          node("id").isValidId
          node("name").isEqualTo("tag")
        }
        node("[1]") {
          node("id").isValidId
          node("name").isEqualTo("tag2")
        }
      }
      node("translations") {
        node("en") {
          node("id").isValidId
          node("text").isEqualTo("EN")
          node("state").isEqualTo("TRANSLATED")
          node("auto").isEqualTo(false)
          node("mtProvider").isEqualTo(null)
        }
        node("de") {
          node("id").isValidId
          node("text").isEqualTo("DE")
          node("state").isEqualTo("TRANSLATED")
        }
      }
      node("screenshots") {
        isArray.hasSize(3)
        node("[1]") {
          node("id").isNumber.isGreaterThan(BigDecimal(0))
          node("filename").isString.endsWith(".jpg").hasSizeGreaterThan(20)
        }
      }
    }

    assertThat(tagService.find(project, "tag")).isNotNull
    assertThat(tagService.find(project, "tag2")).isNotNull

    val key = keyService.findOptional(project.id, keyName).orElseThrow()
    assertThat(tagService.getTagsForKeyIds(listOf(key.id))[key.id]).hasSize(2)
    assertThat(translationService.find(key, testData.english).get().text).isEqualTo("EN")

    val screenshots = screenshotService.findAll(key)
    screenshots.forEach {
      fileStorage.readFile("screenshots/${it.filename}").isNotEmpty()
    }
    assertThat(screenshots).hasSize(3)
    assertThat(imageUploadService.find(screenshotImageIds)).hasSize(0)

    assertThrows<FileStoreException> {
      screenshotImages.forEach {
        fileStorage.readFile(
          "${ImageUploadService.UPLOADED_IMAGES_STORAGE_FOLDER_NAME}/${it.filenameWithExtension}"
        )
      }
    }
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      ApiScope.KEYS_EDIT,
      ApiScope.TRANSLATIONS_EDIT,
      ApiScope.SCREENSHOTS_UPLOAD,
      ApiScope.SCREENSHOTS_DELETE
    ]
  )
  @Test
  fun `updates key with translations and tags and screenshots`() {
    val keyName = "super_key"

    val screenshotImages = (1..3).map { imageUploadService.store(screenshotFile, userAccount!!) }
    val screenshotImageIds = screenshotImages.map { it.id }
    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        tags = listOf("tag", "tag2"),
        screenshotUploadedImageIds = screenshotImageIds,
        screenshotIdsToDelete = listOf(testData.keyWithReferences.screenshots.first().id)
      )
    ).andIsOk.andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo(keyName)
      node("tags") {
        isArray.hasSize(2)
        node("[0]") {
          node("id").isValidId
          node("name").isEqualTo("tag")
        }
        node("[1]") {
          node("id").isValidId
          node("name").isEqualTo("tag2")
        }
      }
      node("translations") {
        node("en") {
          node("id").isValidId
          node("text").isEqualTo("EN")
          node("state").isEqualTo("TRANSLATED")
        }
        node("de") {
          node("id").isValidId
          node("text").isEqualTo("DE")
          node("state").isEqualTo("TRANSLATED")
        }
      }
      node("screenshots") {
        isArray.hasSize(4)
        node("[1]") {
          node("id").isNumber.isGreaterThan(BigDecimal(0))
          node("filename").isString.endsWith(".jpg").hasSizeGreaterThan(20)
        }
      }
    }

    assertThat(tagService.find(project, "tag")).isNotNull
    assertThat(tagService.find(project, "tag2")).isNotNull

    val key = keyService.findOptional(project.id, keyName).orElseThrow()
    assertThat(tagService.getTagsForKeyIds(listOf(key.id))[key.id]).hasSize(2)
    assertThat(translationService.find(key, testData.english).get().text).isEqualTo("EN")

    val screenshots = screenshotService.findAll(key)
    screenshots.forEach {
      fileStorage.readFile("screenshots/${it.filename}").isNotEmpty()
    }
    assertThat(screenshots).hasSize(3)
    assertThat(imageUploadService.find(screenshotImageIds)).hasSize(0)

    assertThrows<FileStoreException> {
      screenshotImages.forEach {
        fileStorage.readFile(
          "${ImageUploadService.UPLOADED_IMAGES_STORAGE_FOLDER_NAME}/${it.filenameWithExtension}"
        )
      }
    }
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
      .andIsBadRequest.andPrettyPrint.andAssertThatJson {
        node("code").isEqualTo("key_exists")
      }
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
  fun `does not delete multiple if not in project`() {
    projectSupplier = { testData.project2 }
    performProjectAuthDelete("keys/${testData.secondKey.id},${testData.firstKey.id}", null)
      .andIsBadRequest.andAssertThatJson {
        node("code").isEqualTo("key_not_from_project")
      }
  }
}
