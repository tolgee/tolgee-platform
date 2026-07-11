package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BigMetaTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.key.Key
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.bigMeta.KeysDistanceDto
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.Logging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.measureTime

class BigMetaControllerTest :
  ProjectAuthControllerTest("/v2/projects/"),
  Logging {
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

    getDistances(listOf(testData.yepKey.id)).assert.hasSize(1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it performs well`() {
    val keys = testData.addLotOfData()
    saveTestDataAndPrepare()

    measureTime {
      storeAndAssertSize(keys, 0, 30, 354)
      storeAndAssertSize(keys, 500, 100, 1409)
      storeAndAssertSize(keys, 10, 200, 3155)
      storeAndAssertSize(keys, 800, 50, 3710)
      storeAndAssertSize(keys, 800, 50, 3710)
    }.inWholeSeconds.assert.isLessThan(5)
  }

  private fun storeAndAssertSize(
    allKeys: List<Key>,
    drop: Int,
    take: Int,
    expectedSize: Int,
  ) {
    val keyIds = allKeys.map { it.id }

    val stored = storeLotOfBigMeta(allKeys, drop, take)
    waitForNotThrowing(pollTime = 50, timeout = 2000) {
      val distances = getDistances(keyIds)
      assertAllHaveAtLeast20Distances(stored, distances)
      getDistances(keyIds).assert.hasSize(expectedSize)
    }
  }

  private fun getDistances(keyIds: List<Long>) = bigMetaService.findExistingKeysDistancesDtosByIds(keyIds)

  private fun assertAllHaveAtLeast20Distances(
    stored: List<Key>,
    distances: Set<KeysDistanceDto>,
  ) {
    val distancesPerKey =
      stored.associate { storedKey ->
        val filtered =
          distances.filter { distance ->
            distance.key1Id == storedKey.id || distance.key2Id == storedKey.id
          }
        storedKey.id to filtered
      }

    distancesPerKey.values.assert.allSatisfy { it.assert.hasSizeGreaterThanOrEqualTo(20) }
  }

  private fun storeLotOfBigMeta(
    keys: List<Key>,
    drop: Int,
    take: Int,
  ): List<Key> {
    val toStore = keys.drop(drop).take(take)
    performProjectAuthPost(
      "big-meta",
      mapOf(
        "relatedKeysInOrder" to
          toStore.reversed().map {
            mapOf(
              "namespace" to it.namespace,
              "keyName" to it.name,
            )
          },
      ),
    ).andIsOk
    return toStore
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
