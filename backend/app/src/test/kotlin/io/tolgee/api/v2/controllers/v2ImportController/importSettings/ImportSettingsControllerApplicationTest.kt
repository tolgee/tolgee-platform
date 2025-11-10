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

  @Value("classpath:import/android/strings_params_everywhere.xml")
  lateinit var androidFile: Resource

  @Value("classpath:import/apple/params_everywhere_cs.xliff")
  lateinit var appleXliffFile: Resource

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testData.projectBuilder.addGerman()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates placeholders for po file`() {
    saveAndPrepare()
    performImport(project.id, listOf("example.po" to poFile))
    assertTranslation(
      "%d page read.",
      "{0, plural,\none {Eine Seite gelesen wurde.}\nother {# Seiten gelesen wurden.}\n}",
    )
    assertTranslation(
      "Welcome back, %1${'$'}s! Your last visit was on %2${'$'}s",
      "Willkommen zurück, {0}! Dein letzter Besuch war am {1}",
    )

    applySettings(overrideKeyDescriptions = false, convertPlaceholdersToIcu = false, createNewKeys = true)
    assertTranslation(
      "%d page read.",
      "{value, plural,\none {Eine Seite gelesen wurde.}\nother {%d Seiten gelesen wurden.}\n}",
    )
    assertTranslation(
      "Welcome back, %1${'$'}s! Your last visit was on %2${'$'}s",
      "Willkommen zurück, %1${'$'}s! Dein letzter Besuch war am %2${'$'}s",
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates placeholders for android file`() {
    saveAndPrepare()
    performImport(project.id, listOf("strings_params_everywhere.xml" to androidFile))
    assertTranslation(
      "dogs_count",
      "{0, plural,\none {# dog {1}}\nother {# dogs {1}}\n}",
    )
    assertTranslation(
      "string_array[0]",
      "First item {0, number}",
    )
    assertTranslation(
      "string_array[1]",
      "Second item {0, number}",
    )
    assertTranslation("with_params", "{0, number} {3} {2, number, .00} {3, number, scientific} %+d")
    applySettings(overrideKeyDescriptions = false, convertPlaceholdersToIcu = false, createNewKeys = true)
    assertTranslation(
      "dogs_count",
      "{value, plural,\none {%d dog %s}\nother {%d dogs %s}\n}",
    )
    assertTranslation(
      "string_array[0]",
      "First item %d",
    )
    assertTranslation(
      "string_array[1]",
      "Second item %d",
    )
    assertTranslation("with_params", "%d %4${'$'}s %.2f %e %+d")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates placeholders for apple xliff file`() {
    saveAndPrepare()
    performImport(project.id, listOf("params_everywhere.xliff" to appleXliffFile))
    assertTranslation(
      "Hi %lld",
      "Hi {0, number}",
    )
    assertTranslation(
      "Order %lld",
      "{0, plural,\n" +
        "zero {Order # Ticket}\n" +
        "one {Order # Ticket}\n" +
        "other {Order # Tickets}\n" +
        "}",
    )

    applySettings(overrideKeyDescriptions = false, convertPlaceholdersToIcu = false, createNewKeys = true)

    assertTranslation(
      "Hi %lld",
      "Hi %lld",
    )
    assertTranslation(
      "Order %lld",
      "{value, plural,\n" +
        "zero {Order %lld Ticket}\n" +
        "one {Order %lld Ticket}\n" +
        "other {Order %lld Tickets}\n" +
        "}",
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `(gh-3000) import with createNewKeys=false when first key has meta`() {
    saveAndPrepare()
    applySettings(overrideKeyDescriptions = false, convertPlaceholdersToIcu = false, createNewKeys = false)
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
      convertPlaceholdersToIcu = true,
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
    convertPlaceholdersToIcu: Boolean,
    createNewKeys: Boolean,
  ) {
    performProjectAuthPut(
      "import-settings",
      mapOf(
        "overrideKeyDescriptions" to overrideKeyDescriptions,
        "convertPlaceholdersToIcu" to convertPlaceholdersToIcu,
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
