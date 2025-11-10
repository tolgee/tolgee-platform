package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.dtos.RelatedKeyDto
import io.tolgee.dtos.request.KeyInScreenshotPositionDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.exceptions.FileStoreException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.fixtures.satisfies
import io.tolgee.model.enums.AssignableTranslationState
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.ImageUploadService
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.InputStreamSource
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerCreationTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeysTestData

  @Autowired
  lateinit var bigMetaService: BigMetaService

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

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key"))
      .andIsCreated.andPrettyPrint
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("super_key")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with description`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", description = "description"))
      .andIsCreated.andPrettyPrint
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("super_key")
        node("description").isEqualTo("description")
      }

    executeInNewTransaction {
      keyService
        .find(project.id, "super_key", null)!!
        .keyMeta!!
        .description.assert
        .isEqualTo("description")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `validates description`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", description = "a".repeat(2001)))
      .andIsBadRequest
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", description = "a".repeat(1999)))
      .andIsCreated
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with size 2000`() {
    performProjectAuthPost("keys", CreateKeyDto(name = KeyControllerTest.MAX_OK_NAME))
      .andIsCreated.andPrettyPrint
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo(KeyControllerTest.MAX_OK_NAME)
      }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_CREATE, Scope.TRANSLATIONS_EDIT])
  @Test
  fun `creates key with keys create scope`() {
    performProjectAuthPost("keys", CreateKeyDto(name = "super_key", translations = mapOf("en" to "", "de" to "")))
      .andIsCreated.andPrettyPrint
      .andAssertThatJson {
        node("id").isValidId
        node("name").isEqualTo("super_key")
      }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_CREATE])
  @Test
  fun `create key with translations require translate permissions`() {
    performProjectAuthPost(
      "keys",
      CreateKeyDto(name = "super_key", translations = mapOf("en" to "hello", "de" to "hello")),
    ).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with translations and tags and screenshots`() {
    val keyName = "super_key"

    val screenshotImages = (1..3).map { imageUploadService.store(screenshotFile, userAccount!!, null) }
    val screenshotImageIds = screenshotImages.map { it.id }
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        tags = listOf("tag", "tag2"),
        screenshotUploadedImageIds = screenshotImageIds,
      ),
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
          "${ImageUploadService.UPLOADED_IMAGES_STORAGE_FOLDER_NAME}/${it.filenameWithExtension}",
        )
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with screenshot meta`() {
    val keyName = "super_key"

    val screenshotImages = (1..3).map { imageUploadService.store(screenshotFile, userAccount!!, null) }
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        tags = listOf("tag", "tag2"),
        screenshots =
          screenshotImages.map {
            KeyScreenshotDto().apply {
              text = "text"
              uploadedImageId = it.id
              positions =
                listOf(
                  KeyInScreenshotPositionDto().apply {
                    x = 100
                    y = 120
                    width = 200
                    height = 300
                  },
                )
            }
          },
      ),
    ).andIsCreated.andPrettyPrint.andAssertThatJson {
      node("screenshots") {
        isArray.hasSize(3)
        node("[1]") {
          node("id").isNumber.isGreaterThan(BigDecimal(0))
          node("filename").isString.endsWith(".png").hasSizeGreaterThan(20)
          node("keyReferences") {
            isArray.hasSize(1)
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
        imageUploadService.find(screenshotImages.map { it.id }),
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

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with big meta`() {
    val keyName = "super_key"

    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = keyName,
        translations = mapOf("en" to "EN", "de" to "DE"),
        relatedKeysInOrder =
          mutableListOf(
            RelatedKeyDto(null, "first_key"),
            RelatedKeyDto(null, "super_key"),
          ),
      ),
    ).andIsCreated.andAssertThatJson {
      node("id").isNumber.satisfies {
        bigMetaService.getCloseKeyIds(it.toLong()).assert.hasSize(1)
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with translation state`() {
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "super_key",
        translations = mapOf("en" to "EN"),
        states = mapOf("en" to AssignableTranslationState.REVIEWED),
      ),
    ).andIsCreated.andAssertThatJson {
      node("id").isNumber.satisfies { id ->
        executeInNewTransaction {
          val key = keyService.get(id.toLong())
          key.translations
            .find { it.language.tag == "en" }!!
            .state.assert
            .isEqualTo(TranslationState.REVIEWED)
        }
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `checks state change language permissions`() {
    prepareTestData(
      scopes = listOf(Scope.KEYS_CREATE, Scope.TRANSLATIONS_EDIT, Scope.TRANSLATIONS_STATE_EDIT),
      stateChangeTags = listOf("cs"),
    )

    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "super_key",
        translations = mapOf("en" to "EN"),
        states = mapOf("en" to AssignableTranslationState.REVIEWED),
      ),
    ).andIsForbidden

    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "super_key",
        translations = mapOf("cs" to "CS"),
        states = mapOf("cs" to AssignableTranslationState.REVIEWED),
      ),
    ).andIsCreated
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `checks state change permissions (missing scope)`() {
    prepareTestData(
      scopes = listOf(Scope.KEYS_CREATE, Scope.TRANSLATIONS_EDIT),
      stateChangeTags = null,
    )

    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "super_key",
        translations = mapOf("cs" to "CS"),
        states = mapOf("cs" to AssignableTranslationState.REVIEWED),
      ),
    ).andIsForbidden

    // this works, because TRANSLATED is the initial state, so we are not changing anytning
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "super_key",
        translations = mapOf("cs" to "CS"),
        states = mapOf("cs" to AssignableTranslationState.TRANSLATED),
      ),
    ).andIsCreated
  }

  private fun prepareTestData(
    scopes: List<Scope>,
    stateChangeTags: List<String>?,
  ) {
    val testData = PermissionsTestData()
    val user =
      testData.addUserWithPermissions(
        scopes = scopes,
        type = null,
        stateChangeLanguageTags = stateChangeTags,
        translateLanguageTags = listOf("en", "cs"),
      )
    testDataService.saveTestData(testData.root)
    userAccount = user
    this.projectSupplier = { testData.projectBuilder.self }
  }
}
