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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import org.testng.annotations.Test

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
open class ProjectServiceTest : AbstractSpringTest() {

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
        val user = dbPopulator.createUserIfNotExists("user")
        val repositories = repositoryService.findAllPermitted(user)
        assertThat(repositories).hasSize(0)
    }

    @Test
    open fun testFindAllSingleRepository() {
        dbPopulator.createUsersAndOrganizations() //create some data
        val repo = dbPopulator.createBase("Hello world", generateUniqueString())
        val repositories = repositoryService.findAllPermitted(repo.userOwner!!)
        assertThat(repositories).hasSize(1)
        assertThat(repositories[0].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    }

    @Test
    @Transactional
    open fun testFindMultiple() {
        val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("helga") //create some data
        val repo = dbPopulator.createBase("Hello world")
        repo.userOwner = userAccountService.get(repo.userOwner!!.id!!).get()
        val organization = usersWithOrganizations[0].organizationRoles[0].organization
        organizationRoleService.grantRoleToUser(repo.userOwner!!, organization!!, OrganizationRoleType.MEMBER)

        val user3 = entityManager.merge(usersWithOrganizations[3])
        entityManager.refresh(user3)

        val organization2 = user3.organizationRoles[0].organization
        organizationRoleService.grantRoleToUser(repo.userOwner!!, organization2!!, OrganizationRoleType.OWNER)
        val repositories = repositoryService.findAllPermitted(repo.userOwner!!)
        assertThat(repositories).hasSize(7)
        assertThat(repositories[6].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
        assertThat(repositories[1].permissionType).isEqualTo(Permission.ProjectPermissionType.VIEW)
        assertThat(repositories[5].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    }

    @Test
    open fun testFindMultiplePermissions() {
        val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("agnes") //create some data
        val repo = dbPopulator.createBase("Hello world")
        repo.userOwner = userAccountService.get(repo.userOwner!!.id!!).get()
        val organization = usersWithOrganizations[0].organizationRoles[0].organization
        organizationRoleService.grantRoleToUser(repo.userOwner!!, organization!!, OrganizationRoleType.MEMBER)

        val user3 = entityManager.merge(usersWithOrganizations[3])
        entityManager.refresh(user3)

        val organization2 = user3.organizationRoles[0].organization
        organizationRoleService.grantRoleToUser(repo.userOwner!!, organization2!!, OrganizationRoleType.OWNER)

        val customPermissionRepo = usersWithOrganizations[0].organizationRoles[0].organization!!.projects[2]
        val customPermissionRepo2 = user3.organizationRoles[0].organization!!.projects[2]
        permissionService.create(
                Permission(
                        user = repo.userOwner,
                        project = customPermissionRepo,
                        type = Permission.ProjectPermissionType.TRANSLATE)
        )
        permissionService.create(
                Permission(
                        user = repo.userOwner,
                        project = customPermissionRepo2,
                        type = Permission.ProjectPermissionType.TRANSLATE)
        )

        val repositories = repositoryService.findAllPermitted(repo.userOwner!!)
        assertThat(repositories).hasSize(7)
        assertThat(repositories[6].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
        assertThat(repositories[2].permissionType).isEqualTo(Permission.ProjectPermissionType.TRANSLATE)
        assertThat(repositories[1].permissionType).isEqualTo(Permission.ProjectPermissionType.VIEW)
        assertThat(repositories[5].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    }
}
