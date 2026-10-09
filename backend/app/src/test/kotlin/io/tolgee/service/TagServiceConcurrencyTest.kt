package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
class TagServiceConcurrencyTest : AbstractSpringTest() {
  private lateinit var testData: TagsTestData
  private val executor = Executors.newFixedThreadPool(2)

  @AfterEach
  fun cleanup() {
    executor.shutdownNow()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `concurrent transactions creating the same tag both succeed`() {
    testData = TagsTestData()
    testDataService.saveTestData(testData.root)
    val firstKeyId = testData.noTagKey.id
    val secondKeyId = testData.existingTagKey2.id
    val firstInserted = CountDownLatch(1)
    val releaseFirst = CountDownLatch(1)

    val first =
      executor.submit {
        executeInNewTransaction {
          tagService.tagKey(keyService.get(firstKeyId), "concurrent tag")
          entityManager.flush()
          firstInserted.countDown()
          releaseFirst.await()
        }
      }
    if (!firstInserted.await(10, TimeUnit.SECONDS)) {
      first.get(0, TimeUnit.SECONDS)
    }

    val second =
      executor.submit {
        executeInNewTransaction {
          tagService.tagKey(keyService.get(secondKeyId), "concurrent tag")
          entityManager.flush()
        }
      }
    waitUntilTagInsertIsBlocked()
    releaseFirst.countDown()

    first.get(10, TimeUnit.SECONDS)
    second.get(10, TimeUnit.SECONDS)

    val tags = tagService.getAllFromProject(testData.projectBuilder.self.id)
    assertThat(tags.filter { it.name == "concurrent tag" }).hasSize(1)
    val tagNamesByKeyId =
      executeInNewTransaction {
        tagService.getTagsForKeyIds(listOf(firstKeyId, secondKeyId)).mapValues { (_, tags) -> tags.map { it.name } }
      }
    assertThat(tagNamesByKeyId[firstKeyId]).contains("concurrent tag")
    assertThat(tagNamesByKeyId[secondKeyId]).contains("concurrent tag")
  }

  private fun waitUntilTagInsertIsBlocked() {
    repeat(100) {
      val blockedCount =
        executeInNewTransaction {
          entityManager
            .createNativeQuery(
              """
              select count(*) from pg_stat_activity
              where datname = current_database() and wait_event_type = 'Lock' and query ilike '%insert into tag%'
              """,
            ).singleResult as Long
        }
      if (blockedCount > 0) {
        return
      }
      Thread.sleep(100)
    }
    throw AssertionError("The second transaction never blocked on the first one's tag insert")
  }
}
