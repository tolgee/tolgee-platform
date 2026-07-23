package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class ProjectContributorsControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: ContributorsTestData

  private val firstAt = Date(1_600_000_000_000)
  private val lastAt = Date(1_600_000_100_000)

  @BeforeEach
  fun setup() {
    testData = ContributorsTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `lists non-member contributors, excludes members, org members, deleted, disabled and anonymous`() {
    recordActivity(testData.contributor.id, firstAt)
    recordActivity(testData.contributor.id, lastAt)
    recordActivity(testData.member.id, lastAt)
    recordActivity(testData.orgMember.id, lastAt)
    recordActivity(testData.deletedContributor.id, lastAt)
    recordActivity(testData.disabledContributor.id, lastAt)
    recordActivity(authorId = null, at = lastAt)
    recordActivity(authorId = 999_999_999L, at = lastAt)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor.id)
        node("_embedded.contributors[0].name").isEqualTo("Cora Contributor")
        node("_embedded.contributors[0].firstContributionAt").isEqualTo(firstAt.time)
        node("_embedded.contributors[0].lastContributionAt").isEqualTo(lastAt.time)
        node("_embedded.contributors[0].username").isAbsent()
        node("_embedded.contributors[0].email").isAbsent()
      }
  }

  @Test
  fun `defaults to last-contribution-desc, honours allowlisted sort, ignores others`() {
    recordActivity(testData.contributor.id, Date(1_600_000_000_000))
    recordActivity(testData.contributor.id, Date(1_600_000_200_000))
    recordActivity(testData.contributor2.id, Date(1_600_000_300_000))

    userAccount = testData.admin

    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(2)
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor2.id)
        node("_embedded.contributors[1].id").isEqualTo(testData.contributor.id)
      }

    performAuthGet("/v2/projects/${testData.project.id}/contributors?sort=firstContributionAt,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor.id)
        node("_embedded.contributors[1].id").isEqualTo(testData.contributor2.id)
      }

    performAuthGet("/v2/projects/${testData.project.id}/contributors?sort=username,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor2.id)
        node("_embedded.contributors[1].id").isEqualTo(testData.contributor.id)
      }
  }

  @Test
  fun `pages the contributor list`() {
    recordActivity(testData.contributor.id, Date(1_600_000_200_000))
    recordActivity(testData.contributor2.id, Date(1_600_000_300_000))

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors?size=1")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor2.id)
        node("page.totalElements").isEqualTo(2)
        node("page.size").isEqualTo(1)
      }
  }

  @Test
  fun `breaks contribution-time ties by user id`() {
    val sameTime = Date(1_600_000_500_000)
    recordActivity(testData.contributor.id, sameTime)
    recordActivity(testData.contributor2.id, sameTime)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(minOf(testData.contributor.id, testData.contributor2.id))
        node("_embedded.contributors[1].id").isEqualTo(maxOf(testData.contributor.id, testData.contributor2.id))
      }
  }

  @Test
  fun `requires MEMBERS_VIEW`() {
    userAccount = testData.member
    performAuthGet("/v2/projects/${testData.project.id}/contributors").andIsForbidden
  }

  private fun recordActivity(
    authorId: Long?,
    at: Date,
  ) {
    currentDateProvider.forcedDate = at
    executeInNewTransaction {
      entityManager.persist(
        ActivityRevision().apply {
          this.projectId = testData.project.id
          this.authorId = authorId
        },
      )
      entityManager.flush()
    }
  }
}
