package io.tolgee.service

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.BigMetaTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BigMetaServiceTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `it deletes references`() {
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

    executeInNewTransaction {
      keyService.delete(testData.yepKey.id)
    }

    waitForNotThrowing(pollTime = 1000) {
      bigMetaService.findExistingKeysDistancesDtosByIds(listOf(testData.yepKey.id)).assert.hasSize(0)
    }
  }
}
