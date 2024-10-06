package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BigMetaTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.key.Key
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import io.tolgee.util.infoMeasureTime
import io.tolgee.util.logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.measureTime

class BigMetaControllerTest : ProjectAuthControllerTest("/v2/projects/"), Logging {
  lateinit var testData: BigMetaTestData

  @Autowired
  lateinit var bigMetaService: BigMetaService

  @BeforeEach
  fun setup() {
    testData = BigMetaTestData()
  }

  private fun saveTestDataAndPrepare() {
    testDataService.saveTestData(testData.root)
    projectSupplier = { testData.project }
    userAccount = testData.userAccount
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it stores`() {
    saveTestDataAndPrepare()
    performProjectAuthPost(
      "big-meta",
      mapOf(
        "relatedKeysInOrder" to
          listOf(
            mapOf(
              "keyName" to "key",
            ),
            mapOf(
              "namespace" to "yep",
              "keyName" to "key",
            ),
          ),
      ),
    ).andIsOk

    bigMetaService.findExistingKeysDistancesDtosByIds(listOf(testData.yepKey.id)).assert.hasSize(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it performs well`() {
    val keys = testData.addLotOfData()
    testData.addLotOfReferences(keys)
    saveTestDataAndPrepare()

    logger.infoMeasureTime("it performs well time 1") {
      storeLogOfBigMeta(keys, 500, 100)
    }

    logger.infoMeasureTime("it performs well time 2") {
      storeLogOfBigMeta(keys, 500, 100)
    }

    logger.infoMeasureTime("it performs well time 3") {
      storeLogOfBigMeta(keys, 10, 200)
    }

    logger.infoMeasureTime("it performs well time 4") {
      storeLogOfBigMeta(keys, 800, 50)
    }

    measureTime {
      storeLogOfBigMeta(keys, 800, 50)
    }.inWholeSeconds.assert.isLessThan(10)

    bigMetaService.findExistingKeysDistancesDtosByIds(keys.map { it.id }).assert.hasSize(104790)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it performs well (large)`() {
    val keys = testData.addLotOfData()
    testData.addLotOfReferences(keys)
    saveTestDataAndPrepare()

    storeLogOfBigMeta(keys, 0, 200)
  }

  private fun storeLogOfBigMeta(
    keys: List<Key>,
    drop: Int,
    take: Int,
  ) {
    performProjectAuthPost(
      "big-meta",
      mapOf(
        "relatedKeysInOrder" to
          keys.drop(drop).take(take).reversed().map {
            mapOf(
              "namespace" to it.namespace,
              "keyName" to it.name,
            )
          },
      ),
    ).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns correct data`() {
    val keys = testData.addLotOfData()
    testData.addLotOfReferences(keys)
    saveTestDataAndPrepare()

    performProjectAuthGet("keys/${keys.first().id}/big-meta").andIsOk.andAssertThatJson {
      node("_embedded").node("keys").isArray().hasSize(10)
    }
  }
}
