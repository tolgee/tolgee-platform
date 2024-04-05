package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.development.testDataBuilder.data.ProjectLeavingTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class ProjectsControllerLeavingTest : ProjectAuthControllerTest("/v2/projects/") {
  @Test
  @ProjectJWTAuthTestMethod
  fun `user can leave project`() {
    val testData = ProjectLeavingTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.project1nonOwner
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("/leave", null).andIsOk
    assertThat(permissionService.find(projectId = testData.projectBuilder.self.id, userId = userAccount!!.id))
      .isNull()
    assertThat(permissionService.find(projectId = testData.projectBuilder.self.id, userId = testData.user.id))
      .isNotNull
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot leave project with organization role`() {
    val testData = ProjectLeavingTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.userWithOrganizationRole
    projectSupplier = { testData.organizationOwnedProject }
    performProjectAuthPut("/leave", null).andPrettyPrint.andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("cannot_leave_project_with_organization_role")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `resets user preferred organization when leaves organization project with only base permissions`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.kvetoslav
    projectSupplier = { testData.projectBuilder.self }
    performProjectAuthPut("/leave", null).andIsOk
    val preferences = userPreferencesService.find(testData.kvetoslav.id)!!
    assertThat(preferences.preferredOrganization?.id)
      .isNotEqualTo(testData.userAccountBuilder.defaultOrganizationBuilder.self.id)
  }
}
