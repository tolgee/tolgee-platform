package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.service.key.KeyTrashPurgeScheduler
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.Date

class KeyTrashPurgeSchedulerTest : AbstractSpringTest() {
  @Autowired
  lateinit var keyTrashPurgeScheduler: KeyTrashPurgeScheduler

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    setForcedDate(Date())
    testData =
      BaseTestData().apply {
        projectBuilder.apply {
          addKey { name = "key1" }.build {
            addTranslation {
              language = englishLanguage
              text = "Key 1"
            }
          }
          addKey { name = "key2" }.build {
            addTranslation {
              language = englishLanguage
              text = "Key 2"
            }
          }
          addKey { name = "key3" }.build {
            addTranslation {
              language = englishLanguage
              text = "Key 3"
            }
          }
        }
      }
    executeInNewTransaction {
      testDataService.saveTestData(testData.root)
    }
  }

  @AfterEach
  fun cleanup() {
    clearForcedDate()
  }

  @Test
  fun `purges keys older than retention period`() {
    val key1Id =
      testData.projectBuilder.data.keys[0]
        .self.id
    val key2Id =
      testData.projectBuilder.data.keys[1]
        .self.id

    // Soft-delete key1 and key2
    executeInNewTransaction {
      keyService.softDeleteMultiple(listOf(key1Id, key2Id), deletedBy = testData.user)
    }

    // Advance time past the 7-day retention period
    moveCurrentDate(Duration.ofDays(8))

    // Run the purge
    keyTrashPurgeScheduler.purge()

    // Both keys should be permanently deleted
    executeInNewTransaction {
      keyService
        .findOptional(key1Id)
        .isPresent.assert
        .isFalse()
      keyService
        .findOptional(key2Id)
        .isPresent.assert
        .isFalse()
    }
  }

  @Test
  fun `does not purge keys within retention period`() {
    val key1Id =
      testData.projectBuilder.data.keys[0]
        .self.id

    // Soft-delete key1
    executeInNewTransaction {
      keyService.softDeleteMultiple(listOf(key1Id), deletedBy = testData.user)
    }

    // Advance time but stay within retention period
    moveCurrentDate(Duration.ofDays(6))

    // Run the purge
    keyTrashPurgeScheduler.purge()

    // Key should still exist (soft-deleted but not purged)
    executeInNewTransaction {
      val key = keyService.findOptional(key1Id)
      key.isPresent.assert.isTrue()
      key
        .get()
        .deletedAt.assert
        .isNotNull()
    }
  }

  @Test
  fun `purges only expired keys and keeps recent ones`() {
    val key1Id =
      testData.projectBuilder.data.keys[0]
        .self.id
    val key2Id =
      testData.projectBuilder.data.keys[1]
        .self.id
    val key3Id =
      testData.projectBuilder.data.keys[2]
        .self.id

    // Soft-delete key1 now
    executeInNewTransaction {
      keyService.softDeleteMultiple(listOf(key1Id), deletedBy = testData.user)
    }

    // Advance 5 days, then soft-delete key2
    moveCurrentDate(Duration.ofDays(5))
    executeInNewTransaction {
      keyService.softDeleteMultiple(listOf(key2Id), deletedBy = testData.user)
    }

    // Advance 3 more days (total: 8 days since key1 deleted, 3 days since key2 deleted)
    moveCurrentDate(Duration.ofDays(3))

    // Run the purge
    keyTrashPurgeScheduler.purge()

    // key1 should be purged (8 days old)
    executeInNewTransaction {
      keyService
        .findOptional(key1Id)
        .isPresent.assert
        .isFalse()
    }

    // key2 should still exist (only 3 days old)
    executeInNewTransaction {
      val key2 = keyService.findOptional(key2Id)
      key2.isPresent.assert.isTrue()
      key2
        .get()
        .deletedAt.assert
        .isNotNull()
    }

    // key3 should still be active (never deleted)
    executeInNewTransaction {
      val key3 = keyService.findOptional(key3Id)
      key3.isPresent.assert.isTrue()
      key3
        .get()
        .deletedAt.assert
        .isNull()
    }
  }

  @Test
  fun `does nothing when trash is empty`() {
    val key1Id =
      testData.projectBuilder.data.keys[0]
        .self.id

    // Advance time past retention without deleting anything
    moveCurrentDate(Duration.ofDays(8))

    // Run the purge — should not throw
    keyTrashPurgeScheduler.purge()

    // All keys should still be active
    executeInNewTransaction {
      val key1 = keyService.findOptional(key1Id)
      key1.isPresent.assert.isTrue()
      key1
        .get()
        .deletedAt.assert
        .isNull()
    }
  }

  @Test
  fun `does not purge active keys`() {
    val key1Id =
      testData.projectBuilder.data.keys[0]
        .self.id
    val key2Id =
      testData.projectBuilder.data.keys[1]
        .self.id

    // Soft-delete key1, leave key2 active
    executeInNewTransaction {
      keyService.softDeleteMultiple(listOf(key1Id), deletedBy = testData.user)
    }

    // Advance past retention
    moveCurrentDate(Duration.ofDays(8))

    keyTrashPurgeScheduler.purge()

    // key1 should be purged
    executeInNewTransaction {
      keyService
        .findOptional(key1Id)
        .isPresent.assert
        .isFalse()
    }

    // key2 must still be active
    executeInNewTransaction {
      val key2 = keyService.findOptional(key2Id)
      key2.isPresent.assert.isTrue()
      key2
        .get()
        .deletedAt.assert
        .isNull()
    }
  }
}
