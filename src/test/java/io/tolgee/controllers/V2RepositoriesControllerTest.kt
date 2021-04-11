package io.tolgee.controllers

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.fixtures.*
import io.tolgee.model.Permission
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class V2RepositoriesControllerTest : SignedInControllerTest() {

    @Test
    fun getAll() {
        dbPopulator.createBase("one")
        dbPopulator.createBase("two")
        dbPopulator.createOrganization("cool", userAccount!!).let { org ->
            dbPopulator.createRepositoryWithOrganization("org repo", org)
        }

        performAuthGet("/v2/repositories").andPrettyPrint.andAssertThatJson.node("_embedded.repositories").let {
            it.isArray.hasSize(3)
            it.node("[0].userOwner.name").isEqualTo("admin")
            it.node("[0].directPermissions").isEqualTo("MANAGE")
            it.node("[2].userOwner").isEqualTo("null")
            it.node("[2].organizationOwnerName").isEqualTo("cool")
            it.node("[2].organizationOwnerAddressPart").isEqualTo("cool")
        }
    }


    @Test
    fun get() {
        val base = dbPopulator.createBase("one")

        performAuthGet("/v2/repositories/${base.id}").andPrettyPrint.andAssertThatJson.let {
            it.node("userOwner.name").isEqualTo("admin")
            it.node("directPermissions").isEqualTo("MANAGE")
        }
    }

    @Test
    fun getNotPermitted() {
        val base = dbPopulator.createBase("one")

        val account = dbPopulator.createUserIfNotExists("peter")
        logAsUser(account.name!!, initialPassword)

        performAuthGet("/v2/repositories/${base.id}").andIsForbidden
    }

    @Test
    fun getAllUsers() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")
        permissionService.grantFullAccessToRepo(user, repo)

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthGet("/v2/repositories/${repo.id}/users").andPrettyPrint.andAssertThatJson.node("_embedded.users").let {
            it.isArray.hasSize(3)
            it.node("[0].organizationRole").isEqualTo("MEMBER")
            it.node("[1].organizationRole").isEqualTo("OWNER")
            it.node("[2].directPermissions").isEqualTo("MANAGE")
        }
    }

    @Test
    fun setUsersPermissions() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")

        permissionService.create(Permission(user = user, repository = repo, type = Permission.RepositoryPermissionType.VIEW))

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

        permissionService.getRepositoryPermissionType(repo.id, user)
                .let { assertThat(it).isEqualTo(Permission.RepositoryPermissionType.EDIT) }
    }

    @Test
    fun setUsersPermissionsDeletesPermission() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")

        organizationRoleService.grantMemberRoleToUser(user, repo.organizationOwner!!)
        permissionService.create(Permission(user = user, repository = repo, type = Permission.RepositoryPermissionType.VIEW))

        repo.organizationOwner!!.basePermissions = Permission.RepositoryPermissionType.EDIT
        organizationRepository.save(repo.organizationOwner!!)

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${user.id}/set-permissions/EDIT", null).andIsOk

        permissionService.getRepositoryPermissionData(repo.id, user.id!!)
                .let { assertThat(it.directPermissions).isEqualTo(null) }
    }

    @Test
    fun setUsersPermissionsNoAccess() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${user.id}/set-permissions/EDIT", null)
                .andIsBadRequest.andReturn().let{
                    assertThat(it).error().hasCode("user_has_no_repository_access")
                }
    }


    @Test
    fun setUsersPermissionsOwner() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")
        organizationRoleService.grantOwnerRoleToUser(user, repo.organizationOwner!!)

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${user.id}/set-permissions/EDIT", null)
                .andIsBadRequest.andReturn().let{
                    assertThat(it).error().hasCode("user_is_organization_owner")
                }
    }

    @Test
    fun setUsersPermissionsHigherBase() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")
        organizationRoleService.grantMemberRoleToUser(user, repo.organizationOwner!!)

        repo.organizationOwner!!.basePermissions = Permission.RepositoryPermissionType.EDIT
        organizationRepository.save(repo.organizationOwner!!)

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${user.id}/set-permissions/TRANSLATE", null)
                .andIsBadRequest.andReturn().let{
                    assertThat(it).error().hasCode("cannot_set_lower_than_organization_base_permissions")
                }
    }


    @Test
    fun setUsersPermissionsOwn() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${usersAndOrganizations[1].id!!}/set-permissions/EDIT", null)
                .andIsBadRequest.andReturn().let{
                    assertThat(it).error().hasCode("cannot_set_your_own_permissions")
                }
    }

    @Test
    fun revokeUsersAccess() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")

        permissionService.create(Permission(user = user, repository = repo, type = Permission.RepositoryPermissionType.VIEW))

        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${user.id}/revoke-access", null).andIsOk

        permissionService.getRepositoryPermissionType(repo.id, user)
                .let { assertThat(it).isNull() }
    }


    @Test
    fun revokeUsersAccessOwn() {
        val repo = dbPopulator.createBase("base", "jirina")

        logAsUser("jirina")

        performAuthPut("/v2/repositories/${repo.id}/users/${repo.userOwner!!.id}/revoke-access", null)
                .andIsBadRequest.andReturn().let{ assertThat(it).error().hasCode("can_not_revoke_own_permissions")}

    }

    @Test
    fun revokeUsersAccessIsOrganizationMember() {
        val usersAndOrganizations = dbPopulator.createUsersAndOrganizations()
        val repo = usersAndOrganizations[1].organizationRoles[0].organization!!.repositories[0]
        val user = dbPopulator.createUserIfNotExists("jirina")

        organizationRoleService.grantMemberRoleToUser(user, repo.organizationOwner!!)
        logAsUser(usersAndOrganizations[1].name!!)

        performAuthPut("/v2/repositories/${repo.id}/users/${user.id}/revoke-access", null)
                .andIsBadRequest.andReturn().let{ assertThat(it).error().hasCode("user_is_organization_member")}
    }


}
