package io.tolgee.api.v2.controllers.v2ImportController.importSettings

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.key.Key
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.test.web.servlet.ResultActions

class ImportSettingsControllerApplicationTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: BaseTestData

  @Value("classpath:import/po/example.po")
  lateinit var poFile: Resource

  @Value("classpath:import/po/simplePoWithSingleKeyMeta.po")
  lateinit var simplePoWithSingleKeyMeta: Resource

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testData.projectBuilder.addGerman()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `(gh-3000) import with createNewKeys=false when first key has meta`() {
    saveAndPrepare()
    applySettings(overrideKeyDescriptions = false, createNewKeys = false)
    performImport(project.id, listOf("simplePoWithSingleKeyMeta.po" to simplePoWithSingleKeyMeta))
    assertTranslation(
      "simple message",
      "einfache Nachricht",
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't override descriptions when disabled`() {
    val key = createKeyToOverrideDescriptionFor()
    val oldDescription = key.keyMeta!!.description!!
    saveAndPrepare()
    performImportWithSettings(overrideKeyDescriptions = false)
    assertKeyDescription(key, oldDescription)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `overrides descriptions when enabled`() {
    val key = createKeyToOverrideDescriptionFor()
    saveAndPrepare()
    performImportWithSettings(overrideKeyDescriptions = true)
    assertKeyDescription(
      key,
      "This is the text that should appear next to menu accelerators " +
        "* that use the super key. If the text on this key isn't typically " +
        "* translated on keyboards used for your language, don't translate " +
        "* this.",
    )
  }

  private fun assertKeyDescription(
    key: Key,
    description: String,
  ) {
    executeInNewTransaction {
      val keyRefreshed = keyService.get(key.id)
      keyRefreshed.keyMeta!!
        .description.assert
        .isEqualTo(description)
    }
  }

  private fun performImportWithSettings(overrideKeyDescriptions: Boolean) {
    performImport(project.id, listOf("example.po" to poFile))
    applySettings(
      overrideKeyDescriptions = overrideKeyDescriptions,
      createNewKeys = true,
    )
    performProjectAuthPut("import/apply?forceMode=OVERRIDE", null).andIsOk
  }

  private fun createKeyToOverrideDescriptionFor(): Key {
    val keyName =
      "We connect developers and translators around the globe " +
        "in Tolgee for a fantastic localization experience."
    val key =
      testData.projectBuilder
        .addKey(keyName)
        .build {
          addMeta {
            description = "This is a description"
          }
        }.self
    return key
  }

  private fun applySettings(
    overrideKeyDescriptions: Boolean,
    createNewKeys: Boolean,
  ) {
    performProjectAuthPut(
      "import-settings",
      mapOf(
        "overrideKeyDescriptions" to overrideKeyDescriptions,
        "createNewKeys" to createNewKeys,
      ),
    ).andIsOk
  }

  private fun performImport(
    projectId: Long,
    files: List<Pair<String, Resource>>?,
    params: Map<String, Any?> = mapOf(),
  ): ResultActions {
    loginAsAdminIfNotLogged()
    return io.tolgee.util.performImport(mvc, projectId, files, params)
  }

  private fun assertTranslation(
    keyName: String,
    translation: String,
  ) {
    executeInNewTransaction {
      importService
        .find(
          project.id,
          userAccount!!.id,
        )!!
        .files
        .first()
        .languages
        .first()
        .translations
        .find { it.key.name == keyName }!!
        .text.assert
        .isEqualTo(translation)
    }
  }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }
}
