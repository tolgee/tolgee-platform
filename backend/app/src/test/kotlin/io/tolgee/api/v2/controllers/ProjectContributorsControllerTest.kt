package io.tolgee.api.v2.controllers

import io.tolgee.contributors.ContributorActivityRecorder
import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.service.contributor.ProjectContributorService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.Date

class ProjectContributorsControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: ContributorsTestData

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var projectContributorService: ProjectContributorService

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
        node("page.totalElements").isEqualTo(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor.id)
        node("_embedded.contributors[0].name").isEqualTo("Cora Contributor")
        node("_embedded.contributors[0].firstContributionAt").isEqualTo(firstAt.time)
        node("_embedded.contributors[0].lastContributionAt").isEqualTo(lastAt.time)
        node("_embedded.contributors[0].avatar").isObject
        node("_embedded.contributors[0].username").isAbsent()
        node("_embedded.contributors[0].email").isAbsent()
      }
  }

  @Test
  fun `defaults to last-contribution-desc, honours allowlisted sort, ignores others`() {
    recordActivity(testData.contributor.id, Date(1_600_000_200_000))
    recordActivity(testData.contributor.id, Date(1_600_000_300_000))
    recordActivity(testData.contributor2.id, Date(1_600_000_100_000))
    recordActivity(testData.contributor2.id, Date(1_600_000_250_000))

    userAccount = testData.admin

    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(2)
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor.id)
        node("_embedded.contributors[1].id").isEqualTo(testData.contributor2.id)
      }

    performAuthGet("/v2/projects/${testData.project.id}/contributors?sort=firstContributionAt,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor2.id)
        node("_embedded.contributors[1].id").isEqualTo(testData.contributor.id)
      }

    performAuthGet("/v2/projects/${testData.project.id}/contributors?sort=username,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor.id)
        node("_embedded.contributors[1].id").isEqualTo(testData.contributor2.id)
      }

    performAuthGet("/v2/projects/${testData.project.id}/contributors?sort=lastContributionAt,asc")
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

    performAuthGet("/v2/projects/${testData.project.id}/contributors?size=1&page=1")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor.id)
        node("page.number").isEqualTo(1)
      }
  }

  @Test
  fun `returns an empty page for a project with no contributors`() {
    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.publicEmptyProject.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(0)
        node("_embedded").isAbsent()
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
  fun `keeps the newest last-contribution when an older activity arrives afterwards`() {
    val older = Date(1_600_000_100_000)
    val newer = Date(1_600_000_900_000)
    recordActivity(testData.contributor.id, newer)
    recordActivity(testData.contributor.id, older)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].firstContributionAt").isEqualTo(older.time)
        node("_embedded.contributors[0].lastContributionAt").isEqualTo(newer.time)
      }
  }

  @Test
  fun `a real modifying request feeds the live activity pipeline into project_contributor`() {
    userAccount = testData.admin
    performAuthPost(
      "/v2/projects/${testData.project.id}/keys",
      CreateKeyDto(name = "contributor.pipeline.key"),
    ).andIsCreated

    val count =
      jdbcTemplate.queryForObject(
        "select count(*) from project_contributor where project_id = ? and user_id = ?",
        Long::class.java,
        testData.project.id,
        testData.admin.id,
      )
    assertThat(count).isEqualTo(1)
  }

  @Test
  fun `lists a non-member global admin as a contributor (exclusion keys on membership, not server role)`() {
    recordActivity(testData.staffContributor.id, lastAt)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.staffContributor.id)
      }
  }

  @Test
  fun `lists a member of another project on a project they are not a member of`() {
    recordActivity(testData.member.id, lastAt, projectId = testData.publicProject.id)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.publicProject.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.member.id)
      }
  }

  @Test
  fun `lists a contributor who is an org member of an unrelated organization`() {
    recordActivity(testData.foreignOrgContributor.id, lastAt)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.foreignOrgContributor.id)
      }
  }

  @Test
  fun `scopes the list to the requested project`() {
    recordActivity(testData.contributor.id, lastAt)
    recordActivity(testData.contributor2.id, lastAt, projectId = testData.publicProject.id)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.contributor.id)
      }
  }

  @Test
  fun `serializes an empty name and no avatar for a contributor without them`() {
    recordActivity(testData.unnamedContributor.id, lastAt)

    userAccount = testData.admin
    performAuthGet("/v2/projects/${testData.project.id}/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors").isArray.hasSize(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.unnamedContributor.id)
        node("_embedded.contributors[0].name").isEqualTo("")
        node("_embedded.contributors[0].avatar").isEqualTo(null)
      }
  }

  @Test
  fun `allows a member holding exactly MEMBERS_VIEW`() {
    userAccount = testData.membersViewer
    performAuthGet("/v2/projects/${testData.project.id}/contributors").andIsOk
  }

  @Test
  fun `partitions every membership shape into exactly one of the members and contributors lists`() {
    recordActivity(testData.member.id, lastAt)
    recordActivity(testData.noneMember.id, lastAt)
    recordActivity(testData.orgMember.id, lastAt)
    recordActivity(testData.contributor.id, lastAt)

    val members =
      userAccountService
        .getAllInProject(testData.project.id, PageRequest.of(0, 100), null)
        .content
        .map { it.id }
        .toSet()
    val contributors =
      projectContributorService
        .getContributors(testData.project.id, PageRequest.of(0, 100))
        .content
        .map { it.id }
        .toSet()

    listOf(testData.member.id, testData.noneMember.id, testData.orgMember.id).forEach { id ->
      assertThat(members).contains(id)
      assertThat(contributors).doesNotContain(id)
    }
    assertThat(contributors).contains(testData.contributor.id)
    assertThat(members).doesNotContain(testData.contributor.id)
  }

  @Test
  fun `requires MEMBERS_VIEW`() {
    userAccount = testData.member
    performAuthGet("/v2/projects/${testData.project.id}/contributors").andIsForbidden
  }

  private fun recordActivity(
    authorId: Long?,
    at: Date,
    projectId: Long = testData.project.id,
  ) {
    executeInNewTransaction {
      ContributorActivityRecorder.record(entityManager, currentDateProvider, projectId, authorId, at)
    }
  }
}
