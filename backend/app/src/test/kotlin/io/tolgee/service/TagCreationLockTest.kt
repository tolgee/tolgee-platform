package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.service.key.TagService
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@SpringBootTest
class TagCreationLockTest : AbstractSpringTest() {
  private lateinit var testData: TagsTestData
  private lateinit var executor: ExecutorService

  @BeforeEach
  fun setup() {
    testData = TagsTestData()
    testDataService.saveTestData(testData.root)
    executor = Executors.newCachedThreadPool()
  }

  @AfterEach
  fun cleanup() {
    executor.shutdownNow()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `creating a tag does not wait for the creation of a tag in another lock bucket`() {
    TagService
      .tagCreationLockBuckets(listOf("tag-a"))
      .assert
      .doesNotContainAnyElementsOf(TagService.tagCreationLockBuckets(listOf("tag-b")))

    whileAnotherTransactionCreates(listOf("tag-a")) {
      lockCreationInNewTransaction(listOf("tag-b")).get(5, TimeUnit.SECONDS)
    }
  }

  @Test
  fun `creating a tag waits while another transaction creates the same tag`() {
    val waitingCreation =
      whileAnotherTransactionCreates(listOf("tag-a")) {
        val creation = lockCreationInNewTransaction(listOf("tag-a"))
        assertThrows<TimeoutException> { creation.get(2, TimeUnit.SECONDS) }
        creation
      }
    waitingCreation.get(10, TimeUnit.SECONDS)
  }

  @Test
  fun `creating many tags takes a limited number of locks`() {
    advisoryLocksHeldAfterLocking((1..100).map { "many-$it" }).assert.isLessThanOrEqualTo(16)
  }

  @Test
  fun `existing tags take no lock`() {
    advisoryLocksHeldAfterLocking(listOf("existing tag", "existing tag 2")).assert.isEqualTo(0)
  }

  @Test
  fun `lock buckets are in the same sorted order whatever the order of the tag names`() {
    val names = (1..20).map { "tag-$it" }
    val buckets = TagService.tagCreationLockBuckets(names)

    buckets.assert.isEqualTo(buckets.sorted())
    TagService.tagCreationLockBuckets(names.reversed()).assert.isEqualTo(buckets)
  }

  private fun advisoryLocksHeldAfterLocking(tagNames: List<String>): Long =
    executeInNewTransaction {
      tagService.lockCreationOfMissingTags(testData.projectBuilder.self.id, tagNames)
      entityManager
        .createNativeQuery("select count(*) from pg_locks where locktype = 'advisory' and pid = pg_backend_pid()")
        .singleResult as Long
    }

  private fun <T> whileAnotherTransactionCreates(
    tagNames: List<String>,
    fn: () -> T,
  ): T {
    val locked = CountDownLatch(1)
    val release = CountDownLatch(1)
    val holder =
      executor.submit {
        executeInNewTransaction {
          tagService.lockCreationOfMissingTags(testData.projectBuilder.self.id, tagNames)
          locked.countDown()
          release.await(30, TimeUnit.SECONDS)
        }
      }
    try {
      locked.await(10, TimeUnit.SECONDS).assert.isTrue()
      return fn()
    } finally {
      release.countDown()
      holder.get(10, TimeUnit.SECONDS)
    }
  }

  private fun lockCreationInNewTransaction(tagNames: List<String>): Future<*> =
    executor.submit {
      executeInNewTransaction {
        tagService.lockCreationOfMissingTags(testData.projectBuilder.self.id, tagNames)
      }
    }
}
