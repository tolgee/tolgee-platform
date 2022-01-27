package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.ProjectTransferringTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.Permission
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class V2ProjectsControllerTransferringTest : ProjectAuthControllerTest("/v2/projects/") {

  @Test
  @ProjectJWTAuthTestMethod
  fun `transfers from user to user`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("/transfer-to-user/${testData.user2.id}", null).andIsOk
    assertThat(projectService.get(project.id).userOwner!!.id)
      .isEqualTo(testData.user2.id)
    assertThat(permissionService.getProjectPermissionType(project.id, testData.user2.id))
      .isEqualTo(Permission.ProjectPermissionType.MANAGE)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `transfers from organization to user`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.organizationOwnedProject }
    performProjectAuthPut("/transfer-to-user/${testData.user2.id}", null).andIsOk
    assertThat(projectService.get(project.id).userOwner!!.id)
      .isEqualTo(testData.user2.id)
    assertThat(permissionService.getProjectPermissionType(project.id, testData.user2.id))
      .isEqualTo(Permission.ProjectPermissionType.MANAGE)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `transfers from user to organization`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("/transfer-to-organization/${testData.organization.id}", null).andIsOk
    assertThat(projectService.get(project.id).organizationOwner!!.id)
      .isEqualTo(testData.organization.id)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't transfers from user to organization when not owner`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("/transfer-to-organization/${testData.notOwnedOrganization.id}", null)
      .andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't transfer from user to user when not in project`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user2
    projectSupplier = { testData.organizationOwnedProject }
    performProjectAuthPut("/transfer-to-user/${testData.user3.id}", null)
      .andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't transfer to user when not permitted`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(
      testData.root.apply {
        data.projects[0].data.permissions[0].self {
          type = Permission.ProjectPermissionType.VIEW
        }
      }
    )
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("/transfer-to-user/${testData.user2.id}", null).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesn't transfer to organization when not permitted`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(
      testData.root.apply {
        data.projects[0].data.permissions[0].self {
          type = Permission.ProjectPermissionType.VIEW
        }
      }
    )
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("/transfer-to-organization/${testData.organization.id}", null).andIsForbidden
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `provides transfer options`() {
    val testData = ProjectTransferringTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthGet("/transfer-options").andPrettyPrint.andIsOk.andAssertThatJson {
      node("_embedded.transferOptions") {
        isArray
        node("[0].name").isEqualTo("Josef Kajetan")
        node("[1].name").isEqualTo("Owned organization")
        node("[2].name").isEqualTo("Petr Vobtahlo")
      }
    }
  }
}
