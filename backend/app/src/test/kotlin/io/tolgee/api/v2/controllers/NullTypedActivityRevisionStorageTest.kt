package io.tolgee.api.v2.controllers

import io.tolgee.activity.ActivityService
import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.key.Key
import io.tolgee.testing.AuthorizedControllerTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NullTypedActivityRevisionStorageTest : AuthorizedControllerTest() {
  @Autowired
  lateinit var activityService: ActivityService

  private lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData("nulltyped_author", "nulltyped_project")
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `drops a null-typed revision with no modifications but keeps one with modifications`() {
    val before = countRevisions()

    store(withModification = false)
    assertThat(countRevisions()).isEqualTo(before)

    store(withModification = true)
    assertThat(countRevisions()).isEqualTo(before + 1)
  }

  private fun store(withModification: Boolean) {
    executeInNewTransaction {
      val revision =
        ActivityRevision().apply {
          this.projectId = testData.project.id
          this.authorId = testData.user.id
          this.type = null
        }
      activityService.storeActivityData(revision, modifiedEntities(revision, withModification))
    }
  }

  private fun modifiedEntities(
    revision: ActivityRevision,
    withModification: Boolean,
  ): ModifiedEntitiesType {
    if (!withModification) return mutableMapOf()
    return mutableMapOf(
      Key::class to mutableMapOf(1L to ActivityModifiedEntity(revision, Key::class.simpleName!!, 1L)),
    )
  }

  private fun countRevisions(): Long =
    executeInNewTransaction {
      entityManager
        .createQuery(
          "select count(ar) from ActivityRevision ar where ar.projectId = :projectId",
          java.lang.Long::class.java,
        ).setParameter("projectId", testData.project.id)
        .singleResult
        .toLong()
    }
}
