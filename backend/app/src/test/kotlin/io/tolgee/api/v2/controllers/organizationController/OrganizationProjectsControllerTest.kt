package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.development.testDataBuilder.data.PermissionsTestData
import io.tolgee.development.testDataBuilder.data.ProjectTranslationsStatsTestData
import io.tolgee.development.testDataBuilder.data.PublicProjectsStatsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationProjectsControllerTest : AuthorizedControllerTest() {
  @Test
  fun `get all projects with slug`() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[1].username)
    users[1].organizationRoles[0].organization.let { organization ->
      performAuthGet("/v2/organizations/${organization!!.slug}/projects")
        .andIsOk.andAssertThatJson
        .let {
          it.node("_embedded.projects").let { projectsNode ->
            projectsNode.isArray.hasSize(3)
            projectsNode.node("[1].name").isEqualTo("user-2's organization 1 project 2")
            projectsNode.node("[1].organizationOwner.slug").isEqualTo("user-2-s-organization-1")
            projectsNode.node("[1].organizationOwner.name").isEqualTo("user-2's organization 1")
          }
        }
    }
  }

  @Test
  fun `get all projects with id`() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[1].username)
    users[1].organizationRoles[0].organization.let { organization ->
      performAuthGet("/v2/organizations/${organization!!.id}/projects")
        .andIsOk.andAssertThatJson
        .let {
          it.node("_embedded.projects").let { projectsNode ->
            projectsNode.isArray.hasSize(3)
            projectsNode.node("[1].name").isEqualTo("user-2's organization 1 project 2")
            projectsNode.node("[1].organizationOwner.slug").isEqualTo("user-2-s-organization-1")
            projectsNode.node("[1].organizationOwner.name").isEqualTo("user-2's organization 1")
          }
        }
    }
  }

  @Test
  fun `get all projects with stats (id)`() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[1].username)
    users[1].organizationRoles[0].organization.let { organization ->
      performAuthGet("/v2/organizations/${organization!!.id}/projects-with-stats")
        .andIsOk.andPrettyPrint
        .andAssertThatJson {
          node("_embedded.projects") {
            node("[1].stats.languageCount").isEqualTo(2)
            node("[1].stats.keyCount").isEqualTo(0)
          }
        }
    }
  }

  @Test
  fun `get all projects with stats (slug)`() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[1].username)
    users[1].organizationRoles[0].organization.let { organization ->
      performAuthGet("/v2/organizations/${organization!!.slug}/projects-with-stats")
        .andIsOk.andPrettyPrint
        .andAssertThatJson {
          node("_embedded.projects") {
            node("[1].stats.languageCount").isEqualTo(2)
            node("[1].stats.keyCount").isEqualTo(0)
          }
        }
    }
  }

  @Test
  fun `user wint none permissions cannot see the the project`() {
    val testData = PermissionsTestData()
    val user = testData.addUserWithPermissions(type = ProjectPermissionType.NONE)
    testDataService.saveTestData(testData.root)
    userAccount = user
    performAuthGet("/v2/organizations/${testData.organizationBuilder.self.id}/projects-with-stats")
      .andAssertThatJson {
        node("_embedded").isAbsent()
      }
  }

  @Test
  fun `user with no direct permission cannot see the the project in organization with none base permissions`() {
    val testData = PermissionsTestData()
    val user =
      testData.root
        .addUserAccount {
          username = "pavek@stb.cz"
        }.self
    testData.organizationBuilder.self.basePermission.type = ProjectPermissionType.NONE
    testData.organizationBuilder.build {
      addRole {
        this.user = user
        this.type = OrganizationRoleType.MEMBER
      }
    }

    testDataService.saveTestData(testData.root)
    userAccount = user
    performAuthGet("/v2/organizations/${testData.organizationBuilder.self.id}/projects-with-stats")
      .andAssertThatJson {
        node("_embedded").isAbsent()
      }
  }

  @Test
  fun `user with scopes can see the project`() {
    val testData = PermissionsTestData()
    val user = testData.addUserWithPermissions(scopes = listOf(Scope.ADMIN))
    testDataService.saveTestData(testData.root)
    userAccount = user
    performAuthGet("/v2/organizations/${testData.organizationBuilder.self.id}/projects-with-stats")
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.projects").isArray.hasSize(1)
      }
  }

  @Test
  fun `projects-with-stats serializes the public flag`() {
    val testData = PublicProjectsStatsTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.admin.self
    val organizationId = testData.organizationBuilder.self.id

    performAuthGet("/v2/organizations/$organizationId/projects-with-stats?search=alpha")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.projects").isArray.hasSize(1)
        node("_embedded.projects[0].name").isEqualTo("Alpha Public")
        node("_embedded.projects[0].public").isEqualTo(true)
      }

    performAuthGet("/v2/organizations/$organizationId/projects-with-stats?search=beta")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.projects").isArray.hasSize(1)
        node("_embedded.projects[0].name").isEqualTo("Beta Private")
        node("_embedded.projects[0].public").isEqualTo(false)
      }
  }

  @Test
  fun `project with single language should show correct translation percentages`() {
    val testData = ProjectTranslationsStatsTestData()
    val user = testData.admin.self

    testDataService.saveTestData(testData.root)
    userAccount = user
    performAuthGet("/v2/organizations/${testData.organizationBuilder.self.id}/projects-with-stats")
      .andPrettyPrint
      .andAssertThatJson {
        node("_embedded.projects") {
          node("").isArray.hasSize(1)
          node("[0].stats") {
            node("keyCount").isNumber.isEqualTo("1")
            node("translationStatePercentages") {
              node("TRANSLATED").isNumber.isEqualTo("100.0")
              node("UNTRANSLATED").isNumber.isEqualTo("0.0")
            }
          }
        }
      }
  }
}
