package io.tolgee.controllers

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest : SignedInControllerTest() {

    @Test
    fun testGetAll() {
        (0..100).forEach {
            organizationRepository.save(
                    Organization(
                            name = "Org - $it",
                            addressPart = "org$it",
                            description = "Org - description $it",
                            basePermissions = Permission.RepositoryPermissionType.VIEW)
            )
        }

        performAuthGet("/api/organizations?size=100")
                .andIsOk.also { println(it.andReturn().response.contentAsString) }
                .andAssertThatJson.node("_embedded.organizations")
                .also {
                    it.isArray.hasSize(100)
                    it.node("[1]._links.self.href").isEqualTo("http://localhost/api/organizations/org1")
                    it.node("[2].name").isEqualTo("Org - 2")
                    it.node("[2].description").isEqualTo("Org - description 2")
                    it.node("[2].addressPart").isEqualTo("org2")
                    it.node("[2].basePermission").isEqualTo("VIEW")
                }
    }

    @Test
    fun testGetAllUsers() {
        val users = dbPopulator.createUsersAndOrganizations()
        logAsUser(users[0].username!!, initialPassword)
        val organizationId = users[1].organizationMemberRoles[0].organization!!.id
        performAuthGet("/v2/organizations/$organizationId/users").andIsOk
                .also { println(it.andReturn().response.contentAsString) }
                .andAssertThatJson.node("_embedded.usersInOrganization").also {
                    it.isArray.hasSize(2)
                    it.node("[0].organizationRoleType").isEqualTo("OWNER")
                    it.node("[1].organizationRoleType").isEqualTo("MEMBER")
                }
    }


    @Test
    fun testGetAllUsersNotPermitted() {
        val users = dbPopulator.createUsersAndOrganizations()
        val organizationId = users[1].organizationMemberRoles[1].organization!!.id
        performAuthGet("/v2/organizations/$organizationId/users").andIsForbidden
    }
}
