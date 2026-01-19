package io.tolgee.fixtures

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BranchTranslationsTestData
import io.tolgee.dtos.request.ComplexTagKeysRequest
import io.tolgee.dtos.request.KeyId
import io.tolgee.dtos.request.SetDisabledLanguagesRequest
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.dtos.request.key.EditKeyDto
import io.tolgee.dtos.request.key.TagKeyDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.model.enums.AssignableTranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.KeyScreenshotReferenceRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamSource
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.ResultActions

abstract class ProtectedBranchModificationTestBase : ProjectAuthControllerTest("/v2/projects/") {
  protected lateinit var testData: BranchTranslationsTestData

  @Autowired
  lateinit var keyScreenshotReferenceRepository: KeyScreenshotReferenceRepository

  protected val protectedBranchName = "protected"
  protected val mainBranchName = "main"

  @BeforeEach
  open fun setup() {
    testData = BranchTranslationsTestData()
    projectSupplier = { testData.project }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  protected fun expectForbidden(action: () -> ResultActions) = action().andIsForbidden

  protected fun expectOk(action: () -> ResultActions) = action().andIsOk

  protected fun expectCreated(action: () -> ResultActions) = action().andIsCreated

  protected fun translateKey(
    key: String,
    branch: String,
  ) = performProjectAuthPut(
    "/translations",
    SetTranslationsWithKeyDto(
      key,
      null,
      mutableMapOf("en" to "English"),
      branch = branch,
    ),
  )

  protected fun createOrUpdateTranslation(
    key: String,
    branch: String,
  ) = performProjectAuthPost(
    "/translations",
    SetTranslationsWithKeyDto(
      key,
      null,
      mutableMapOf("en" to "English"),
      branch = branch,
    ),
  )

  protected fun setTranslationStateToTranslated(translationId: Long) =
    performProjectAuthPut(
      "/translations/$translationId/set-state/${AssignableTranslationState.TRANSLATED}",
    )

  protected fun dismissAutoTranslatedState(translationId: Long) =
    performProjectAuthPut("/translations/$translationId/dismiss-auto-translated-state")

  protected fun setOutdatedFlag(
    translationId: Long,
    value: Boolean = true,
  ) = performProjectAuthPut("/translations/$translationId/set-outdated-flag/$value")

  protected fun getTranslation(
    key: Key,
    languageTag: String = "en",
  ): Translation {
    val translations = translationService.getTranslations(listOf(key.id), listOf(testData.en.id))
    return translations.find { it.language.tag == languageTag }!!
  }

  protected fun createKey(
    name: String,
    branch: String,
  ) = performProjectAuthPost(
    "/keys",
    CreateKeyDto(
      name = name,
      branch = branch,
    ),
  )

  protected fun editKey(
    keyId: Long,
    newName: String,
  ) = performProjectAuthPut(
    "/keys/$keyId",
    EditKeyDto(
      name = newName,
    ),
  )

  protected fun complexEditKey(
    keyId: Long,
    newName: String,
  ) = performProjectAuthPut(
    "/keys/$keyId/complex-update",
    ComplexEditKeyDto(
      name = newName,
    ),
  )

  protected fun deleteKey(keyId: Long) = performProjectAuthDelete("/keys/$keyId")

  protected fun setDisabledLanguages(
    keyId: Long,
    languageIds: List<Long>,
  ) = performProjectAuthPut(
    "/keys/$keyId/disabled-languages",
    SetDisabledLanguagesRequest(languageIds),
  )

  protected fun tagKey(
    keyId: Long,
    tagName: String,
  ) = performProjectAuthPut(
    "/keys/$keyId/tags",
    TagKeyDto(tagName),
  )

  protected fun removeTag(
    keyId: Long,
    tagId: Long,
  ) = performProjectAuthDelete("/keys/$keyId/tags/$tagId")

  protected fun executeComplexTagOperation(
    keyId: Long,
    branch: String,
    tagName: String,
  ) = performProjectAuthPut(
    "/tag-complex?branch=$branch",
    ComplexTagKeysRequest(
      filterKeys = listOf(KeyId(name = null, namespace = null, id = keyId)),
      filterKeysNot = null,
      filterTag = null,
      filterTagNot = null,
      tagFiltered = listOf(tagName),
      untagFiltered = null,
      tagOther = null,
      untagOther = null,
    ),
  )

  protected fun uploadScreenshot(keyId: Long): ResultActions {
    return performProjectAuthMultipart(
      url = "keys/$keyId/screenshots",
      files =
        listOf(
          MockMultipartFile(
            "screenshot",
            "originalShot.png",
            "image/png",
            screenshotFile.inputStream.readAllBytes(),
          ),
          MockMultipartFile(
            "info",
            "info",
            MediaType.APPLICATION_JSON_VALUE,
            "{}".toByteArray(),
          ),
        ),
    )
  }

  protected fun deleteScreenshots(
    keyId: Long,
    screenshotId: Long,
  ) = performProjectAuthDelete("keys/$keyId/screenshots/$screenshotId")

  private val screenshotFile: InputStreamSource by lazy {
    generateImage()
  }
}
