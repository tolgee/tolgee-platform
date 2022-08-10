package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationProjectsControllerTest : AuthorizedControllerTest() {

  @Test
  fun `get all projects with slug`() {
    val users = dbPopulator.createUsersAndOrganizations()
    loginAsUser(users[1].username)
    users[1].organizationRoles[0].organization.let { organization ->
      performAuthGet("/v2/organizations/${organization!!.slug}/projects")
        .andIsOk.andAssertThatJson.let {
          it.node("_embedded.projects").let { projectsNode ->
            projectsNode.isArray.hasSize(3)
            projectsNode.node("[1].name").isEqualTo("user-2's organization 1 project 2")
            projectsNode.node("[1].organizationOwnerSlug").isEqualTo("user-2-s-organization-1")
            projectsNode.node("[1].organizationOwnerName").isEqualTo("user-2's organization 1")
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
        .andIsOk.andAssertThatJson.let {
          it.node("_embedded.projects").let { projectsNode ->
            projectsNode.isArray.hasSize(3)
            projectsNode.node("[1].name").isEqualTo("user-2's organization 1 project 2")
            projectsNode.node("[1].organizationOwnerSlug").isEqualTo("user-2-s-organization-1")
            projectsNode.node("[1].organizationOwnerName").isEqualTo("user-2's organization 1")
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
        .andIsOk.andPrettyPrint.andAssertThatJson {
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
        .andIsOk.andPrettyPrint.andAssertThatJson {
          node("_embedded.projects") {
            node("[1].stats.languageCount").isEqualTo(2)
            node("[1].stats.keyCount").isEqualTo(0)
          }
        }
    }
  }
}
