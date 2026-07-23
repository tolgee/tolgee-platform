package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.service.contributor.ProjectContributorService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import java.util.Date

@SpringBootTest
@AutoConfigureMockMvc
class PublicProjectsControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: PublicProjectsControllerTestData

  @Autowired
  private lateinit var projectContributorService: ProjectContributorService

  @BeforeEach
  fun setup() {
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `lists only public projects to an anonymous visitor`() {
    performGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        isArray.hasSize(2)
        node("[0].id").isEqualTo(testData.otherOrgPublicProject.id)
        node("[1].id").isEqualTo(testData.publicProject.id)
      }
    }
  }

  @Test
  fun `a public row carries stats, org and NONE permission for an anonymous visitor`() {
    performGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects[1]") {
        node("id").isEqualTo(testData.publicProject.id)
        node("public").isEqualTo(true)
        node("organizationOwner.name").isEqualTo("test_username")
        node("stats.keyCount").isEqualTo(0)
        node("languages").isArray.hasSize(1)
        node("directPermission").isEqualTo(null)
        node("organizationRole").isEqualTo(null)
        node("computedPermission.type").isEqualTo("NONE")
        node("computedPermission.scopes").isArray.hasSize(0)
      }
    }
  }

  @Test
  fun `search matches the project name case-insensitively`() {
    performGet("/v2/public/projects/with-stats?search=other ORG").andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        isArray.hasSize(1)
        node("[0].id").isEqualTo(testData.otherOrgPublicProject.id)
      }
    }
  }

  @Test
  fun `search matches the organization name case-insensitively`() {
    performGet("/v2/public/projects/with-stats?search=vibrant").andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        isArray.hasSize(1)
        node("[0].id").isEqualTo(testData.otherOrgPublicProject.id)
      }
    }
  }

  @Test
  fun `pages the public projects`() {
    performGet("/v2/public/projects/with-stats?size=1").andIsOk.andAssertThatJson {
      node("_embedded.projects").isArray.hasSize(1)
      node("page.totalElements").isEqualTo(2)
      node("page.size").isEqualTo(1)
    }
  }

  @Test
  fun `the with-stats payload reflects the public flag of each project`() {
    userAccount = testData.user
    performAuthGet("/v2/projects/with-stats?sort=id").andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        node("[0].id").isEqualTo(testData.privateProject.id)
        node("[0].public").isEqualTo(false)
        node("[1].id").isEqualTo(testData.publicProject.id)
        node("[1].public").isEqualTo(true)
      }
    }
  }

  @Test
  fun `a logged-in member sees their real permission on a public row (per-user join wired)`() {
    userAccount = testData.user
    performAuthGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        node("[0].id").isEqualTo(testData.otherOrgPublicProject.id)
        node("[0].organizationRole").isEqualTo(null)
        node("[0].computedPermission.type").isEqualTo("VIEW")
        node("[0].computedPermission.origin").isEqualTo("COMMUNITY")
        node("[1].id").isEqualTo(testData.publicProject.id)
        node("[1].organizationRole").isEqualTo("OWNER")
        node("[1].computedPermission.type").isEqualTo("MANAGE")
      }
    }
  }

  @Test
  fun `a logged-in user sees their direct project permission on a public row`() {
    userAccount = testData.directPermissionUser
    performAuthGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects[0]") {
        node("id").isEqualTo(testData.otherOrgPublicProject.id)
        node("organizationRole").isEqualTo(null)
        node("directPermission.type").isEqualTo("TRANSLATE")
        node("computedPermission.type").isEqualTo("TRANSLATE")
      }
    }
  }

  @Test
  fun `excludes a soft-deleted public project`() {
    performGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects").isArray.hasSize(2)
    }
  }

  @Test
  fun `a logged-in non-member gets the community permission on a public row`() {
    userAccount = testData.nonMember
    performAuthGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        isArray.hasSize(2)
        node("[1].id").isEqualTo(testData.publicProject.id)
        node("[1].directPermission").isEqualTo(null)
        node("[1].organizationRole").isEqualTo(null)
        node("[1].computedPermission.type").isEqualTo("VIEW")
        node("[1].computedPermission.origin").isEqualTo("COMMUNITY")
      }
    }
  }

  @Test
  fun `excludes a public project with no base language without mutating it`() {
    performGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects").isArray.hasSize(2)
    }
    baseLanguageId(testData.noBaseLanguageProject.id).assert.isNull()
  }

  @Test
  fun `excludes a public project whose base language is soft-deleted without mutating it`() {
    val baseBefore = baseLanguageId(testData.softDeletedBaseProject.id)
    performGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects").isArray.hasSize(2)
    }
    baseLanguageId(testData.softDeletedBaseProject.id).assert.isEqualTo(baseBefore)
  }

  @Test
  fun `excludes a public project with no organization owner`() {
    performGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects").isArray.hasSize(2)
    }
  }

  @Test
  fun `excludes a public project of a soft-deleted organization`() {
    performGet("/v2/public/projects/with-stats").andIsOk.andAssertThatJson {
      node("_embedded.projects").isArray.hasSize(2)
    }
  }

  @Test
  fun `hasPublicProjects matches the public listing visibility exactly`() {
    val defaultOrg = testData.userAccountBuilder.defaultOrganizationBuilder.self
    projectService.hasPublicProjects(defaultOrg.id).assert.isTrue()
    projectService.hasPublicProjects(testData.otherOrg.id).assert.isTrue()
    projectService.hasPublicProjects(testData.noPublicOrg.id).assert.isFalse()
    projectService.hasPublicProjects(testData.noBaseLangOnlyOrg.id).assert.isFalse()
    projectService.hasPublicProjects(testData.softDeletedBaseLangOnlyOrg.id).assert.isFalse()
    projectService.hasPublicProjects(testData.deletedProjectOnlyOrg.id).assert.isFalse()
    projectService.hasPublicProjects(testData.softDeletedOrg.id).assert.isFalse()
  }

  @Test
  fun `filterContributed lists only public projects the user contributed to as a non-member`() {
    recordActivity(testData.publicProject.id, testData.nonMember.id)

    userAccount = testData.nonMember
    performAuthGet("/v2/public/projects/with-stats?filterContributed=true").andIsOk.andAssertThatJson {
      node("_embedded.projects") {
        isArray.hasSize(1)
        node("[0].id").isEqualTo(testData.publicProject.id)
      }
    }
  }

  @Test
  fun `filterContributed excludes a contributed project the user is a member of`() {
    recordActivity(testData.publicProject.id, testData.user.id)

    userAccount = testData.user
    performAuthGet("/v2/public/projects/with-stats?filterContributed=true").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @Test
  fun `filterContributed returns nothing to an anonymous visitor`() {
    recordActivity(testData.publicProject.id, testData.nonMember.id)

    performGet("/v2/public/projects/with-stats?filterContributed=true").andIsOk.andAssertThatJson {
      node("page.totalElements").isEqualTo(0)
    }
  }

  @Test
  fun `hasCommunityContributions is true for a non-member contributor, false for a member`() {
    recordActivity(testData.publicProject.id, testData.nonMember.id)
    recordActivity(testData.publicProject.id, testData.user.id)

    projectContributorService.hasCommunityContributions(testData.nonMember.id).assert.isTrue()
    projectContributorService.hasCommunityContributions(testData.user.id).assert.isFalse()
  }

  private fun recordActivity(
    projectId: Long,
    authorId: Long?,
    at: Date = Date(1_600_000_000_000),
  ) {
    currentDateProvider.forcedDate = at
    executeInNewTransaction {
      entityManager.persist(
        ActivityRevision().apply {
          this.projectId = projectId
          this.authorId = authorId
        },
      )
      entityManager.flush()
    }
  }

  private fun baseLanguageId(projectId: Long): Long? =
    executeInNewTransaction {
      (
        entityManager
          .createNativeQuery("select base_language_id from project where id = :id")
          .setParameter("id", projectId)
          .singleResult as Number?
      )?.toLong()
    }
}
