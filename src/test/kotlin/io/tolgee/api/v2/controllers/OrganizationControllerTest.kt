package io.tolgee.api.v2.controllers

import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.assertions.Assertions.assertThatThrownBy
import io.tolgee.controllers.SignedInControllerTest
import io.tolgee.dtos.request.OrganizationDto
import io.tolgee.dtos.request.OrganizationInviteUserDto
import io.tolgee.dtos.request.SetOrganizationRoleDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.fixtures.*
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerTest : SignedInControllerTest() {

    lateinit var dummyDto: OrganizationDto
    lateinit var dummyDto2: OrganizationDto

    @BeforeMethod
    fun setup() {
        resetDto()
        this.userAccount = userAccountService.getByUserName(username = userAccount!!.username).get()
    }

    private fun resetDto() {
        dummyDto = OrganizationDto(
                "Test org",
                "This is description",
                "test-org",
                Permission.ProjectPermissionType.VIEW)

        dummyDto2 = OrganizationDto(
                "Test org 2",
                "This is description 2",
                "test-org-2",
                Permission.ProjectPermissionType.VIEW)
    }

    @Test
    fun testGetAll() {
        val users = dbPopulator.createUsersAndOrganizations()

        logAsUser(users[1].name!!, initialPassword)

        performAuthGet("/api/organizations?size=100")
                .andPrettyPrint.andAssertThatJson.let {
                    it.node("_embedded.organizations").let {
                        it.isArray.hasSize(6)
                        it.node("[0].name").isEqualTo("user 2's organization 1")
                        it.node("[0].basePermissions").isEqualTo("VIEW")
                        it.node("[0].currentUserRole").isEqualTo("OWNER")
                    }
                }
    }

    @Test
    fun testGetAllFilterOwned() {
        val users = dbPopulator.createUsersAndOrganizations()

        logAsUser(users[1].name!!, initialPassword)

        performAuthGet("/api/organizations?size=100&filterCurrentUserOwner=true")
                .andPrettyPrint.andAssertThatJson.let {
                    it.node("_embedded.organizations").let {
                        it.isArray.hasSize(1)
                        it.node("[0].name").isEqualTo("user 2's organization 1")
                        it.node("[0].basePermissions").isEqualTo("VIEW")
                        it.node("[0].currentUserRole").isEqualTo("OWNER")
                    }
                }
    }

    @Test
    fun testGetAllSort() {
        val users = dbPopulator.createUsersAndOrganizations()

        logAsUser(users[1].name!!, initialPassword)

        performAuthGet("/api/organizations?size=100&sort=basePermissions,desc&sort=name,desc")
                .andPrettyPrint
                .andAssertThatJson
                .node("_embedded.organizations").node("[0].name").isEqualTo("user 4's organization 3")
    }

    @Test
    fun testGetAllUsers() {
        val users = dbPopulator.createUsersAndOrganizations()
        logAsUser(users[0].username!!, initialPassword)
        val organizationId = users[1].organizationRoles[0].organization!!.id
        performAuthGet("/v2/organizations/$organizationId/users").andIsOk
                .also { println(it.andReturn().response.contentAsString) }
                .andAssertThatJson.node("_embedded.usersInOrganization").also {
                    it.isArray.hasSize(2)
                    it.node("[0].organizationRole").isEqualTo("OWNER")
                    it.node("[1].organizationRole").isEqualTo("MEMBER")
                }
    }


    @Test
    fun testGetOneWithUrl() {
        this.organizationService.create(dummyDto, userAccount!!).let {
            performAuthGet("/v2/organizations/${it.addressPart}").andIsOk.andAssertThatJson.let {
                it.node("name").isEqualTo(dummyDto.name)
                it.node("description").isEqualTo(dummyDto.description)
            }
        }
    }

    @Test
    fun testGetOneWithId() {
        this.organizationService.create(dummyDto, userAccount!!).let { organization ->
            performAuthGet("/v2/organizations/${organization.id}").andIsOk.andAssertThatJson.let {
                it.node("name").isEqualTo(dummyDto.name)
                it.node("id").isEqualTo(organization.id)
                it.node("description").isEqualTo(dummyDto.description)
                it.node("basePermissions").isEqualTo(dummyDto.basePermissions.name)
                it.node("addressPart").isEqualTo(dummyDto.addressPart)
            }
        }
    }

    @Test
    fun testGetOnePermissions() {
        this.organizationService.create(dummyDto, userAccount!!).let {
            performAuthGet("/v2/organizations/${it.id}").andIsOk.andAssertThatJson.let {
                it.node("name").isEqualTo(dummyDto.name)
                it.node("description").isEqualTo(dummyDto.description)
            }
        }
    }

    @Test
    fun testGetAllUsersNotPermitted() {
        val users = dbPopulator.createUsersAndOrganizations()
        val organizationId = users[1].organizationRoles[0].organization!!.id
        performAuthGet("/v2/organizations/$organizationId/users").andIsForbidden
    }

    @Test
    fun testCreate() {
        performAuthPost(
                "/v2/organizations",
                dummyDto
        ).andIsCreated.andPrettyPrint.andAssertThatJson.let {
            it.node("name").isEqualTo("Test org")
            it.node("addressPart").isEqualTo("test-org")
            it.node("_links.self.href").isEqualTo("http://localhost/v2/organizations/test-org")
            it.node("id").isNumber.satisfies {
                organizationService.get(it.toLong()) is Organization
            }
        }
    }

    @Test
    fun testCreateAddressPartValidation() {
        this.organizationService.create(dummyDto2.also { it.addressPart = "hello-1" }, userAccount!!)

        performAuthPost(
                "/v2/organizations",
                dummyDto.also { it.addressPart = "hello-1" }
        ).andIsBadRequest.andAssertError.isCustomValidation.hasMessage("address_part_not_unique")
    }

    @Test
    fun testCreateNotAllowed() {
        this.tolgeeProperties.authentication.userCanCreateOrganizations = false
        performAuthPost("/v2/organizations",
                dummyDto
        ).andIsForbidden
        this.tolgeeProperties.authentication.userCanCreateOrganizations = true
    }

    @Test
    fun testCreateValidation() {
        performAuthPost("/v2/organizations",
                dummyDto.also { it.addressPart = "" }
        ).andIsBadRequest.let {
            assertThat(it.andReturn()).error().isStandardValidation.onField("addressPart")
        }
        performAuthPost("/v2/organizations",
                dummyDto.also { it.name = "" }
        ).andIsBadRequest.let {
            assertThat(it.andReturn()).error().isStandardValidation.onField("name")
        }

        performAuthPost("/v2/organizations",
                dummyDto.also { it.addressPart = "sahsaldlasfhl " }
        ).andIsBadRequest.let {
            assertThat(it.andReturn()).error().isStandardValidation.onField("addressPart")
        }

        performAuthPost("/v2/organizations",
                dummyDto.also { it.addressPart = "a" }
        ).andIsBadRequest.let {
            assertThat(it.andReturn()).error().isStandardValidation.onField("addressPart")
        }
    }

    @Test
    fun testCreateGeneratesAddressPart() {
        performAuthPost("/v2/organizations",
                dummyDto.also { it.addressPart = null }
        ).andIsCreated.andAssertThatJson.node("addressPart").isEqualTo("test-org")
    }

    @Test
    fun testEdit() {
        this.organizationService.create(dummyDto, userAccount!!).let {
            performAuthPut(
                    "/v2/organizations/${it.id}",
                    dummyDto.also { organization ->
                        organization.name = "Hello"
                        organization.addressPart = "hello-1"
                        organization.basePermissions = Permission.ProjectPermissionType.TRANSLATE
                        organization.description = "This is changed description"
                    }
            ).andIsOk.andPrettyPrint.andAssertThatJson.let {
                it.node("name").isEqualTo("Hello")
                it.node("addressPart").isEqualTo("hello-1")
                it.node("_links.self.href").isEqualTo("http://localhost/v2/organizations/hello-1")
                it.node("basePermissions").isEqualTo("TRANSLATE")
                it.node("description").isEqualTo("This is changed description")
            }
        }
    }

    @Test
    fun testEditAddressPartValidation() {
        this.organizationService.create(dummyDto2.also { it.addressPart = "hello-1" }, userAccount!!)

        this.organizationService.create(dummyDto, userAccount!!).let { organization ->
            performAuthPut(
                    "/v2/organizations/${organization.id}",
                    dummyDto.also { organizationDto ->
                        organizationDto.addressPart = "hello-1"
                    }
            ).andIsBadRequest.andAssertError.isCustomValidation.hasMessage("address_part_not_unique")
        }
    }

    @Test
    fun testDelete() {
        val organization2 = this.organizationService.create(dummyDto2, userAccount!!)
        this.organizationService.create(dummyDto, userAccount!!).let {
            performAuthDelete("/v2/organizations/${it.id}", null)
            assertThat(organizationService.get(it.id!!)).isNull()
            assertThat(organizationService.get(organization2.id!!)).isNotNull
        }
    }

    @Test
    fun testLeaveOrganization() {
        this.organizationService.create(dummyDto, userAccount!!).let {
            organizationRepository.findAllPermitted(userAccount!!.id!!, PageRequest.of(0, 20)).content.let {
                assertThat(it).isNotEmpty
            }

            organizationRoleService.grantOwnerRoleToUser(dbPopulator.createUserIfNotExists("secondOwner"), it)

            performAuthPut("/v2/organizations/${it.id}/leave", null)

            organizationRepository.findAllPermitted(userAccount!!.id!!, PageRequest.of(0, 20)).content.let {
                assertThat(it).isEmpty()
            }
        }
    }

    @Test
    fun testLeaveOrganizationNoOtherOwner() {
        this.organizationService.create(dummyDto, userAccount!!).let {
            organizationRepository.findAllPermitted(userAccount!!.id!!, PageRequest.of(0, 20)).content.let {
                assertThat(it).isNotEmpty
            }

            performAuthPut("/v2/organizations/${it.id}/leave", null)
                    .andIsBadRequest
                    .andAssertError
                    .isCustomValidation.hasMessage("organization_has_no_other_owner")
        }
    }

    @Test
    fun testSetUserRole() {
        this.organizationService.create(dummyDto, userAccount!!).let { organization ->
            dbPopulator.createUserIfNotExists("superuser").let { createdUser ->
                OrganizationRole(
                        user = createdUser,
                        organization = organization,
                        type = OrganizationRoleType.OWNER).let { createdMemberRole ->
                    organizationRoleRepository.save(createdMemberRole)
                    performAuthPut(
                            "/v2/organizations/${organization.id!!}/users/${createdUser.id}/set-role",
                            SetOrganizationRoleDto(OrganizationRoleType.MEMBER)
                    ).andIsOk
                    createdMemberRole.let { assertThat(it.type).isEqualTo(OrganizationRoleType.MEMBER) }
                }
            }
        }
    }

    @Test
    fun testRemoveUser() {
        this.organizationService.create(dummyDto, userAccount!!).let { organization ->
            dbPopulator.createUserIfNotExists("superuser").let { createdUser ->
                OrganizationRole(
                        user = createdUser,
                        organization = organization,
                        type = OrganizationRoleType.OWNER).let { createdMemberRole ->
                    organizationRoleRepository.save(createdMemberRole)
                    performAuthDelete(
                            "/v2/organizations/${organization.id!!}/users/${createdUser.id}",
                            SetOrganizationRoleDto(OrganizationRoleType.MEMBER)
                    ).andIsOk
                    organizationRoleRepository.findByIdOrNull(createdMemberRole.id!!).let {
                        assertThat(it).isNull()
                    }
                }
            }
        }
    }


    @Test
    fun testGetAllRepositories() {
        val users = dbPopulator.createUsersAndOrganizations()
        logAsUser(users[1].username!!, initialPassword)
        users[1].organizationRoles[0].organization.let { organization ->
            performAuthGet("/v2/organizations/${organization!!.addressPart}/repositories")
                    .andIsOk.andAssertThatJson.let {
                        it.node("_embedded.repositories").let { repositoriesNode ->
                            repositoriesNode.isArray.hasSize(3)
                            repositoriesNode.node("[1].name").isEqualTo("user 2's organization 1 project 2")
                            repositoriesNode.node("[1].organizationOwnerAddressPart").isEqualTo("user-2-s-organization-1")
                            repositoriesNode.node("[1].organizationOwnerName").isEqualTo("user 2's organization 1")
                        }
                    }
        }
    }

    @Test
    fun testGetAllInvitations() {
        val helloUser = dbPopulator.createUserIfNotExists("hellouser")

        this.organizationService.create(dummyDto, helloUser).let { organization ->
            val invitation = invitationService.create(organization, OrganizationRoleType.MEMBER)
            logAsUser("hellouser", initialPassword)
            performAuthGet("/v2/organizations/${organization.id}/invitations")
                    .andIsOk.andAssertThatJson.let {
                        it.node("_embedded.organizationInvitations").let { repositoriesNode ->
                            repositoriesNode.isArray.hasSize(1)
                            repositoriesNode.node("[0].id").isEqualTo(invitation.id)
                        }
                    }
        }
    }


    @Test
    fun testInviteUser() {
        val helloUser = dbPopulator.createUserIfNotExists("hellouser")
        logAsUser(helloUser.username!!, initialPassword)

        this.organizationService.create(dummyDto, helloUser).let { organization ->
            val body = OrganizationInviteUserDto(roleType = OrganizationRoleType.MEMBER)
            performAuthPut("/v2/organizations/${organization.id}/invite", body).andPrettyPrint.andAssertThatJson.let {
                it.node("code").isString.hasSize(50).satisfies {
                    invitationService.getInvitation(it) //it throws on not found
                }
                it.node("type").isEqualTo("MEMBER")
            }
        }
    }

    @Test
    fun testAcceptInvitation() {
        val helloUser = dbPopulator.createUserIfNotExists("hellouser")

        this.organizationService.create(dummyDto, helloUser).let { organization ->
            val invitation = invitationService.create(organization, OrganizationRoleType.MEMBER)
            val invitedUser = dbPopulator.createUserIfNotExists("invitedUser")
            logAsUser(invitedUser.username!!, initialPassword)
            performAuthGet("/api/invitation/accept/${invitation.code}").andIsOk
            assertThatThrownBy { invitationService.getInvitation(invitation.code) }
                    .isInstanceOf(BadRequestException::class.java)
            organizationRoleService.isUserMemberOrOwner(invitedUser.id!!, organization.id!!).let {
                assertThat(it).isTrue
            }
        }
    }

    @Test
    fun testDeleteInvitation() {
        val helloUser = dbPopulator.createUserIfNotExists("hellouser")

        this.organizationService.create(dummyDto, helloUser).let { organization ->
            val invitation = invitationService.create(organization, OrganizationRoleType.MEMBER)
            logAsUser(helloUser.username!!, initialPassword)
            performAuthDelete("/api/invitation/${invitation.id!!}", null).andIsOk
            assertThatThrownBy { invitationService.getInvitation(invitation.code) }
                    .isInstanceOf(BadRequestException::class.java)
        }
    }
}
