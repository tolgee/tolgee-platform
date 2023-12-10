package io.tolgee.api.v2.controllers

import io.tolgee.AbstractServerAppProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BigMetaTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class BigMetaControllerTest : AbstractServerAppProjectAuthControllerTest("/v2/projects/") {

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
        "relatedKeysInOrder" to listOf(
          mapOf(
            "keyName" to "key"
          ),
          mapOf(
            "namespace" to "yep",
            "keyName" to "key"
          ),
        )
      )
    ).andIsOk

    bigMetaService.findExistingKeysDistancesByIds(listOf(testData.yepKey.id)).assert.hasSize(1)
  }

  @OptIn(ExperimentalTime::class)
  @Test
  @ProjectJWTAuthTestMethod
  fun `it performs well`() {
    val keys = testData.addLotOfData()
    saveTestDataAndPrepare()

    val time = measureTime {
      performProjectAuthPost(
        "big-meta",
        mapOf(
          "relatedKeysInOrder" to keys.take(100).map {
            mapOf(
              "namespace" to it.namespace,
              "keyName" to it.name
            )
          }
        )
      ).andIsOk
    }

    time.inWholeSeconds.assert.isLessThan(10)
    bigMetaService.findExistingKeysDistancesByIds(keys.map { it.id }).assert.hasSize(20790)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it returns correct data`() {
    val keys = testData.addLotOfData()
    saveTestDataAndPrepare()

    performProjectAuthGet("keys/${keys.first().id}/big-meta").andIsOk.andAssertThatJson {
      node("_embedded").node("keys").isArray().hasSize(10)
    }
  }
}
