package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.key.Key
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.util.Date

@Import(BatchJobBaseConfiguration::class)
class BatchRestoreKeysTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var batchJobTestBase: BatchJobTestBase

  lateinit var testData: BaseTestData
  lateinit var softDeletedKey: Key
  lateinit var conflictingSoftDeletedKey: Key
  lateinit var activeKeyWithSameName: Key

  @BeforeEach
  fun setup() {
    batchJobTestBase.setup()
    testData = BaseTestData()
    testData.projectBuilder.apply {
      softDeletedKey =
        addKey {
          name = "deleted-key"
          deletedAt = Date()
        }.self

      // A soft-deleted key whose name conflicts with an active key
      conflictingSoftDeletedKey =
        addKey {
          name = "conflicting-key"
          deletedAt = Date()
        }.self

      activeKeyWithSameName =
        addKey {
          name = "conflicting-key"
        }.self
    }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  @AfterEach
  fun tearDown() {
    batchJobTestBase.tearDown()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it restores soft-deleted keys`() {
    val result =
      performProjectAuthPost(
        "start-batch-job/restore-keys",
        mapOf("keyIds" to listOf(softDeletedKey.id)),
      ).andIsOk

    batchJobTestBase.waitForJobCompleted(result)

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      executeInNewTransaction {
        val key = keyService.find(softDeletedKey.id)
        key.assert.isNotNull
        key!!.deletedAt.assert.isNull()
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it fails to restore when active key with same name exists`() {
    val result =
      performProjectAuthPost(
        "start-batch-job/restore-keys",
        mapOf("keyIds" to listOf(conflictingSoftDeletedKey.id)),
      ).andIsOk

    batchJobTestBase.waitForJobCompleted(result)

    waitForNotThrowing(pollTime = 1000, timeout = 10000) {
      executeInNewTransaction {
        // The conflicting key should still be soft-deleted
        val key = keyService.find(conflictingSoftDeletedKey.id)
        key.assert.isNotNull
        key!!.deletedAt.assert.isNotNull
      }
    }
  }
}
