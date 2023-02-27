package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.dtos.request.KeyInScreenshotPositionDto
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.exceptions.FileStoreException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.ApiScope
import io.tolgee.service.ImageUploadService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.InputStreamSource
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerUpdateTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: KeysTestData

  val screenshotFile: InputStreamSource by lazy {
    generateImage(2000, 3000)
  }

  @BeforeEach
  fun setup() {
    testData = KeysTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
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

    val screenshotImages = (1..3).map { imageUploadService.store(screenshotFile, userAccount!!, null) }
    val screenshotImageIds = screenshotImages.map { it.id }
    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        tags = listOf("tag", "tag2"),
        screenshotUploadedImageIds = screenshotImageIds,
        screenshotIdsToDelete = listOf(testData.screenshot.id)
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
        isArray.hasSize(3)
        node("[1]") {
          node("id").isNumber.isGreaterThan(BigDecimal(0))
          node("filename").isString.endsWith(".png").hasSizeGreaterThan(20)
        }
      }
    }

    assertThat(tagService.find(project, "tag")).isNotNull
    assertThat(tagService.find(project, "tag2")).isNotNull

    val key = keyService.get(project.id, keyName, null)
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

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [
      ApiScope.KEYS_EDIT,
      ApiScope.TRANSLATIONS_EDIT,
      ApiScope.SCREENSHOTS_UPLOAD,
      ApiScope.SCREENSHOTS_DELETE
    ]
  )
  fun `updates key screenshots with meta`() {
    val keyName = "super_key"

    val screenshotImages = (1..3).map { imageUploadService.store(screenshotFile, userAccount!!, null) }
    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        tags = listOf("tag", "tag2"),
        screenshotIdsToDelete = listOf(testData.screenshot.id),
        screenshotsToAdd = screenshotImages.map {
          KeyScreenshotDto().apply {
            text = "text"
            uploadedImageId = it.id
            positions = listOf(
              KeyInScreenshotPositionDto().apply {
                x = 100
                y = 120
                width = 200
                height = 300
              }
            )
          }
        }
      )
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("screenshots") {
        isArray.hasSize(3)
        node("[1]") {
          node("id").isNumber.isGreaterThan(BigDecimal(0))
          node("filename").isString.endsWith(".png").hasSizeGreaterThan(20)
          node("keyReferences") {
            isArray
            node("[0]") {
              node("keyId").isValidId
              node("position") {
                node("x").isEqualTo(71)
                node("y").isEqualTo(85)
                node("width").isEqualTo(141)
                node("height").isEqualTo(212)
              }
              node("keyName").isEqualTo("super_key")
              node("keyNamespace").isEqualTo(null)
              node("originalText").isEqualTo("text")
            }
          }
        }
      }
    }

    executeInNewTransaction {
      val key = keyService.get(project.id, keyName, null)
      val screenshots = screenshotService.findAll(key)
      screenshots.forEach {
        fileStorage.readFile("screenshots/${it.filename}").isNotEmpty()
      }
      assertThat(screenshots).hasSize(3)
      assertThat(
        imageUploadService.find(screenshotImages.map { it.id })
      ).hasSize(0)
      screenshots.forEach {
        val position = it.keyScreenshotReferences[0].positions!![0]
        assertThat(position.x).isEqualTo(71)
        assertThat(position.y).isEqualTo(85)
        assertThat(position.width).isEqualTo(141)
        assertThat(position.height).isEqualTo(212)
      }
    }
  }
}
