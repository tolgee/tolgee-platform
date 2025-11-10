package io.tolgee.api.v2.controllers.v2KeyController

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ResolvableImportTestData
import io.tolgee.dtos.request.ImageUploadInfoDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import kotlin.properties.Delegates
import kotlin.time.measureTime

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerResolvableImportTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: ResolvableImportTestData
  var uploadedImageId by Delegates.notNull<Long>()
  var uploadedImageId2 by Delegates.notNull<Long>()

  @Value("classpath:keyImportRequest.json")
  lateinit var realData: Resource

  @BeforeEach
  fun setup() {
    testData = ResolvableImportTestData()
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.projectBuilder.self }
    userAccount = testData.user
    uploadedImageId =
      imageUploadService
        .store(
          generateImage(),
          userAccount!!,
          ImageUploadInfoDto(location = "My cool frame"),
        ).id
    uploadedImageId2 =
      imageUploadService
        .store(
          generateImage(),
          testData.viewOnlyUser,
          ImageUploadInfoDto(location = "My cool frame"),
        ).id
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports`() {
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "de" to
                    mapOf(
                      "text" to "changed",
                      "resolution" to "OVERRIDE",
                    ),
                  "en" to
                    mapOf(
                      "text" to "new",
                      "resolution" to "NEW",
                    ),
                ),
              "screenshots" to
                listOf(
                  mapOf(
                    "text" to "Oh oh Oh",
                    "uploadedImageId" to uploadedImageId,
                    "positions" to
                      listOf(
                        mapOf(
                          "x" to 100,
                          "y" to 150,
                          "width" to 80,
                          "height" to 100,
                        ),
                        mapOf(
                          "x" to 500,
                          "y" to 200,
                          "width" to 30,
                          "height" to 20,
                        ),
                      ),
                  ),
                ),
            ),
            mapOf(
              "name" to "key-2",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "new",
                      "resolution" to "KEEP",
                    ),
                ),
              "screenshots" to
                listOf(
                  mapOf(
                    "text" to "Oh oh Oh",
                    "uploadedImageId" to uploadedImageId,
                    "positions" to
                      listOf(
                        mapOf(
                          "x" to 100,
                          "y" to 150,
                          "width" to 80,
                          "height" to 100,
                        ),
                      ),
                  ),
                ),
            ),
          ),
      ),
    ).andIsOk.andAssertThatJson {
      node("keys").isArray.hasSize(2)
      node("screenshots") {
        node(uploadedImageId.toString()) {
          node("id").isNumber
          node("filename").isString
        }
      }
    }

    screenshotService.findByIdIn(listOf(testData.key2Screenshot.id)).assert.isNotEmpty
    screenshotService.findByIdIn(listOf(testData.key1and2Screenshot.id)).assert.isEmpty()

    executeInNewTransaction {
      assertTranslationText("namespace-1", "key-1", "de", "changed")
      assertTranslationText("namespace-1", "key-1", "en", "new")
      assertTranslationText("namespace-1", "key-2", "en", "existing translation")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it imports a lot of data (and set outdated) in time`() {
    val lotOfKeys = testData.addLotOfKeys(5000)
    testDataService.saveTestData(testData.root)

    val toImport =
      lotOfKeys.map {
        mapOf(
          "name" to it.name,
          "translations" to
            mapOf(
              "en" to
                mapOf(
                  "text" to "new",
                  "resolution" to "OVERRIDE",
                ),
            ),
        )
      }

    val time =
      measureTime {
        performProjectAuthPost(
          "keys/import-resolvable",
          mapOf(
            "keys" to toImport,
          ),
        ).andIsOk
      }

    time.inWholeSeconds.assert.isLessThan(10)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns errors correctly`() {
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "de" to
                    mapOf(
                      "text" to "changed",
                    ),
                  "en" to
                    mapOf(
                      "text" to "new",
                      "resolution" to "KEEP",
                    ),
                ),
            ),
          ),
      ),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("import_keys_error")
      node("params") {
        isArray.hasSize(2)
        node("[0]") {
          node("[0]").isEqualTo("translation_exists")
          node("[1]").isEqualTo("namespace-1")
          node("[2]").isEqualTo("key-1")
          node("[3]").isEqualTo("de")
        }
        node("[1]") {
          node("[0]").isEqualTo("translation_not_found")
          node("[1]").isEqualTo("namespace-1")
          node("[2]").isEqualTo("key-1")
          node("[3]").isEqualTo("en")
        }
      }
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with real data`() {
    val data: Map<*, *> = jacksonObjectMapper().readValue(realData.inputStream, Map::class.java)
    performProjectAuthPost(
      "keys/import-resolvable",
      data,
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `requires language translate permissions`() {
    userAccount = testData.enOnlyUser
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "de" to
                    mapOf(
                      "text" to "changed",
                    ),
                ),
            ),
          ),
      ),
    ).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cannot add without key create permission`() {
    userAccount = testData.viewOnlyUser
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-10",
            ),
          ),
      ),
    ).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cannot upload screenshot without the permission`() {
    userAccount = testData.enOnlyUser
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-1",
              "screenshots" to
                listOf(
                  mapOf(
                    "text" to "Oh oh Oh",
                    "uploadedImageId" to uploadedImageId,
                    "positions" to
                      listOf(
                        mapOf(
                          "x" to 100,
                          "y" to 150,
                          "width" to 80,
                          "height" to 100,
                        ),
                        mapOf(
                          "x" to 500,
                          "y" to 200,
                          "width" to 30,
                          "height" to 20,
                        ),
                      ),
                  ),
                ),
            ),
          ),
      ),
    ).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `can create with create permission`() {
    userAccount = testData.keyCreateOnlyUser
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-8",
            ),
          ),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `can translate with translate permission`() {
    userAccount = testData.translateOnlyUser
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "changed",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `option force-override works`() {
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "key-1",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "de" to
                    mapOf(
                      "text" to "new",
                      "resolution" to "FORCE_OVERRIDE",
                    ),
                  "en" to
                    mapOf(
                      "text" to "new",
                      "resolution" to "FORCE_OVERRIDE",
                    ),
                ),
            ),
            mapOf(
              "name" to "key-2",
              "namespace" to "namespace-1",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "existing translation",
                      "resolution" to "FORCE_OVERRIDE",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      assertTranslationText("namespace-1", "key-1", "de", "new")
      assertTranslationText("namespace-1", "key-1", "en", "new")
      assertTranslationText("namespace-1", "key-2", "en", "existing translation")
    }
  }

  fun assertTranslationText(
    namespace: String?,
    keyName: String,
    languageTag: String,
    expectedText: String,
  ) {
    projectService
      .get(testData.projectBuilder.self.id)
      .keys
      .find { it.name == keyName && it.namespace?.name == namespace }!!
      .translations
      .find { it.language.tag == languageTag }!!
      .text
      .assert
      .isEqualTo(
        expectedText,
      )
  }
}
