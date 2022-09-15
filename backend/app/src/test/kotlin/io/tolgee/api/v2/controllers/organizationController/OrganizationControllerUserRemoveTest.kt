package io.tolgee.api.v2.controllers.organizationController

import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull

@SpringBootTest
@AutoConfigureMockMvc
class OrganizationControllerUserRemoveTest : BaseOrganizationControllerTest() {

  @AfterEach
  fun resetProps() {
    tolgeeProperties.authentication.userCanCreateOrganizations = true
  }

  @Test
  fun testRemoveUser() {
    withOwnerInOrganization { organization, owner, role ->
      organizationRoleRepository.save(role)
      performAuthDelete("/v2/organizations/${organization.id}/users/${owner.id}", null).andIsOk
      organizationRoleRepository.findByIdOrNull(role.id!!).let {
        assertThat(it).isNull()
      }
    }
  }

  @Test
  fun `remove user resets preferred`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.kvetoslav
    performAuthDelete("/v2/organizations/${testData.jirinaOrg.id}/users/${testData.jirina.id}").andIsOk

    assertThat(userPreferencesService.find(testData.jirina.id)!!.preferredOrganization!!.id)
      .isNotEqualTo(testData.jirinaOrg.id)
  }

  @Test
  fun `doesn't create new preferred preferred`() {
    tolgeeProperties.authentication.userCanCreateOrganizations = false
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.franta

    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("name").isString.isEqualTo("test_username")
    }

    assertThat(userPreferencesService.find(testData.franta.id)!!.preferredOrganization)
      .isNotNull

    userAccount = testData.user

    performAuthPut(
      "/v2/projects/${testData.projectBuilder.self.id}/users/${testData.franta.id}/revoke-access",
      null
    ).andIsOk

    assertThat(userPreferencesService.find(testData.franta.id)!!.preferredOrganization)
      .isNull()

    userAccount = testData.franta

    performAuthGet("/v2/preferred-organization").andIsForbidden
  }
}
