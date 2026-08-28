package io.tolgee.api.v2.controllers

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.PublicProjectsControllerTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

class OrganizationEntitlementVisibilityTest : AuthorizedControllerTest() {
  lateinit var testData: PublicProjectsControllerTestData

  @MockitoBean
  @Autowired
  lateinit var enabledFeaturesProvider: EnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = PublicProjectsControllerTestData()
    testDataService.saveTestData(testData.root)
    doReturn(emptyArray<Feature>()).whenever(enabledFeaturesProvider).get(any())
    doReturn(arrayOf(Feature.PREMIUM_SUPPORT)).whenever(enabledFeaturesProvider).get(testData.otherOrg.id)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a member reads the organization's paid entitlements`() {
    userAccount = testData.otherOrgMember

    adoptOtherOrg()

    performAuthGet(PREFERRED_ORGANIZATION_URL).andIsOk.andAssertThatJson {
      node("enabledFeatures").isEqualTo(listOf("PREMIUM_SUPPORT"))
    }
  }

  @Test
  fun `a non-member reaching the organization through its public projects reads its entitlements`() {
    userAccount = testData.nonMember

    adoptOtherOrg()

    // A project inherits the features of its own organization, whoever is looking at it.
    performAuthGet(PREFERRED_ORGANIZATION_URL).andIsOk.andAssertThatJson {
      node("limitedView").isEqualTo(true)
      node("enabledFeatures").isEqualTo(listOf("PREMIUM_SUPPORT"))
    }
  }

  @Test
  fun `a server admin reads the entitlements of an organization they hold no role in`() {
    userAccount = testData.serverAdmin

    adoptOtherOrg()

    performAuthGet(PREFERRED_ORGANIZATION_URL).andIsOk.andAssertThatJson {
      node("limitedView").isEqualTo(false)
      node("enabledFeatures").isEqualTo(listOf("PREMIUM_SUPPORT"))
    }
  }

  @Test
  fun `a server supporter reads the entitlements of an organization they hold no role in`() {
    userAccount = testData.serverSupporter

    adoptOtherOrg()

    performAuthGet(PREFERRED_ORGANIZATION_URL).andIsOk.andAssertThatJson {
      node("limitedView").isEqualTo(false)
      node("enabledFeatures").isEqualTo(listOf("PREMIUM_SUPPORT"))
    }
  }

  @Test
  fun `a direct project permission user reads the entitlements without a role`() {
    userAccount = testData.directPermissionUser

    adoptOtherOrg()

    performAuthGet(PREFERRED_ORGANIZATION_URL).andIsOk.andAssertThatJson {
      node("currentUserRole").isEqualTo(null)
      node("limitedView").isEqualTo(false)
      node("enabledFeatures").isEqualTo(listOf("PREMIUM_SUPPORT"))
    }
  }

  @Test
  fun `initial data carries the entitlements of an organization reached through its public projects`() {
    userAccount = testData.nonMember

    adoptOtherOrg()

    performAuthGet(INITIAL_DATA_URL).andIsOk.andAssertThatJson {
      node("preferredOrganization.limitedView").isEqualTo(true)
      node("preferredOrganization.enabledFeatures").isEqualTo(listOf("PREMIUM_SUPPORT"))
    }
  }

  @Test
  fun `initial data carries the entitlements of an organization the viewer belongs to`() {
    userAccount = testData.otherOrgMember

    adoptOtherOrg()

    performAuthGet(INITIAL_DATA_URL).andIsOk.andAssertThatJson {
      node("preferredOrganization.limitedView").isEqualTo(false)
      node("preferredOrganization.enabledFeatures").isEqualTo(listOf("PREMIUM_SUPPORT"))
    }
  }

  private fun adoptOtherOrg() {
    executeInNewTransaction {
      userPreferencesService.setPreferredOrganization(
        organizationService.get(testData.otherOrg.id),
        userAccountService.get(userAccount!!.id),
      )
    }
  }

  companion object {
    private const val PREFERRED_ORGANIZATION_URL = "/v2/preferred-organization"
    private const val INITIAL_DATA_URL = "/v2/public/initial-data"
  }
}
