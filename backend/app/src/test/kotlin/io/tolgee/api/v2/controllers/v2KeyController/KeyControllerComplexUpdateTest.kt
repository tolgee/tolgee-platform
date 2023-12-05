package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.dtos.request.KeyInScreenshotPositionDto
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.exceptions.FileStoreException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.model.enums.AssignableTranslationState
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
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
class KeyControllerComplexUpdateTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: KeysTestData

  val screenshotFile: InputStreamSource by lazy {
    generateImage(2000, 3000)
  }

  @BeforeEach
  fun setup() {
    testData = KeysTestData()
    testData.projectBuilder.addCzech()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.TRANSLATIONS_EDIT
    ]
  )
  @Test
  fun `complex edit validates change state permissions`() {
    val keyName = "super_key"

    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        states = mapOf("en" to AssignableTranslationState.REVIEWED),
      )
    ).andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_CREATE,
      Scope.TRANSLATIONS_EDIT,
      Scope.TRANSLATIONS_STATE_EDIT
    ]
  )
  @Test
  fun `complex edit modifies state`() {
    // new translation
    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = "key_with_referecnces",
        translations = mapOf("en" to "EN", "de" to "DE"),
        states = mapOf("en" to AssignableTranslationState.REVIEWED),
      )
    ).andIsOk

    assertKeyWithReferencesState("en", TranslationState.REVIEWED)
    assertKeyWithReferencesState("de", TranslationState.TRANSLATED)

    // existing translation
    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = "key_with_referecnces",
        translations = mapOf("en" to "EN", "de" to "DE"),
        states = mapOf("de" to AssignableTranslationState.REVIEWED),
      )
    ).andIsOk

    assertKeyWithReferencesState("en", TranslationState.REVIEWED)
    assertKeyWithReferencesState("de", TranslationState.REVIEWED)


    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = "key_with_referecnces",
        translations = mapOf("cs" to "CS"),
        states = mapOf(),
      )
    ).andIsOk

    assertKeyWithReferencesState("en", TranslationState.REVIEWED)
    assertKeyWithReferencesState("de", TranslationState.REVIEWED)
    assertKeyWithReferencesState("cs", TranslationState.TRANSLATED)
  }

  private fun assertKeyWithReferencesState(laguageTag: String, state: TranslationState) {
    executeInNewTransaction {
      val key = keyService.find(testData.keyWithReferences.id)
      assertThat(key).isNotNull
      val enTranslationState = key!!.translations.find { it.language.tag == laguageTag }!!.state
      assertThat(enTranslationState).isEqualTo(state)
    }
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_EDIT,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_UPLOAD,
      Scope.SCREENSHOTS_DELETE
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

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.TRANSLATIONS_EDIT,
    ]
  )
  @Test
  fun `can modify permitted language translations`() {
    this.userAccount = testData.enOnlyUserAccount
    performProjectAuthPut(
      "keys/${testData.firstKey.id}/complex-update",
      ComplexEditKeyDto(
        name = testData.firstKey.name,
        translations = mapOf("en" to "Oh yes!"),
        tags = listOf()
      )
    ).andIsOk
  }

  @Test
  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_EDIT,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_UPLOAD,
      Scope.SCREENSHOTS_DELETE
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
