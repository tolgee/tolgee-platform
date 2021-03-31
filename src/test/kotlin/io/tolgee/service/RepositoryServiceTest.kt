/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.testng.annotations.Test

@SpringBootTest
open class RepositoryServiceTest : AbstractSpringTest() {

    @Test
    open fun testFindAllPermitted() {
        val users = dbPopulator.createUsersAndOrganizations()
        dbPopulator.createBase("Test", users[3].username!!)
        val repositories = repositoryService.findAllPermitted(users[3])
        assertThat(repositories).hasSize(10)
    }


    @Test
    open fun testFindAllEmpty() {
        dbPopulator.createUsersAndOrganizations() //create some data
        val user = dbPopulator.createUser("user")
        val repositories = repositoryService.findAllPermitted(user)
        assertThat(repositories).hasSize(0)
    }

    @Test
    open fun testFindAllSingleRepository() {
        dbPopulator.createUsersAndOrganizations() //create some data
        val repo = dbPopulator.createBase("Hello world", generateUniqueString())
        val repositories = repositoryService.findAllPermitted(repo.userOwner!!)
        assertThat(repositories).hasSize(1)
        assertThat(repositories[0].permissionType).isEqualTo(Permission.RepositoryPermissionType.MANAGE)
    }

    @Test
    @Transactional
    open fun testFindMultiple() {
        val usersWithOrganizations = dbPopulator.createUsersAndOrganizations() //create some data
        val repo = dbPopulator.createBase("Hello world")
        repo.userOwner = userAccountService.get(repo.userOwner!!.id!!).get()
        val organization = usersWithOrganizations[0].organizationMemberRoles[0].organization
        organizationMemberRoleService.grantRoleToUser(repo.userOwner!!, organization!!, OrganizationRoleType.MEMBER)
        val organization2 = usersWithOrganizations[3].organizationMemberRoles[0].organization
        organizationMemberRoleService.grantRoleToUser(repo.userOwner!!, organization2!!, OrganizationRoleType.OWNER)
        val repositories = repositoryService.findAllPermitted(repo.userOwner!!)
        assertThat(repositories).hasSize(7)
        assertThat(repositories[6].permissionType).isEqualTo(Permission.RepositoryPermissionType.MANAGE)
        assertThat(repositories[1].permissionType).isEqualTo(Permission.RepositoryPermissionType.VIEW)
        assertThat(repositories[5].permissionType).isEqualTo(Permission.RepositoryPermissionType.MANAGE)
    }

    @Test
    open fun testFindMultiplePermissions() {
        val usersWithOrganizations = dbPopulator.createUsersAndOrganizations() //create some data
        val repo = dbPopulator.createBase("Hello world")
        repo.userOwner = userAccountService.get(repo.userOwner!!.id!!).get()
        val organization = usersWithOrganizations[0].organizationMemberRoles[0].organization
        organizationMemberRoleService.grantRoleToUser(repo.userOwner!!, organization!!, OrganizationRoleType.MEMBER)
        val organization2 = usersWithOrganizations[3].organizationMemberRoles[0].organization
        organizationMemberRoleService.grantRoleToUser(repo.userOwner!!, organization2!!, OrganizationRoleType.OWNER)

        val customPermissionRepo = usersWithOrganizations[0].organizationMemberRoles[0].organization!!.repositories[2]
        val customPermissionRepo2 = usersWithOrganizations[3].organizationMemberRoles[0].organization!!.repositories[2]
        permissionService.create(
                Permission(
                        user = repo.userOwner,
                        repository = customPermissionRepo,
                        type = Permission.RepositoryPermissionType.TRANSLATE)
        )
        permissionService.create(
                Permission(
                        user = repo.userOwner,
                        repository = customPermissionRepo2,
                        type = Permission.RepositoryPermissionType.TRANSLATE)
        )

        val repositories = repositoryService.findAllPermitted(repo.userOwner!!)
        assertThat(repositories).hasSize(7)
        assertThat(repositories[6].permissionType).isEqualTo(Permission.RepositoryPermissionType.MANAGE)
        assertThat(repositories[2].permissionType).isEqualTo(Permission.RepositoryPermissionType.TRANSLATE)
        assertThat(repositories[1].permissionType).isEqualTo(Permission.RepositoryPermissionType.VIEW)
        assertThat(repositories[5].permissionType).isEqualTo(Permission.RepositoryPermissionType.MANAGE)
    }
}
