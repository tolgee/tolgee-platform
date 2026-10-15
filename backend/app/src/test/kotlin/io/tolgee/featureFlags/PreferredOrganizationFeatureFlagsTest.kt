package io.tolgee.featureFlags

import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.featureFlags.OrganizationFeatureGrantService
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PreferredOrganizationFeatureFlagsTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var organizationFeatureGrantService: OrganizationFeatureGrantService

  private lateinit var testData: OrganizationTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    internalProperties.allowPlanFeatureGrants = true
  }

  @AfterEach
  fun cleanup() {
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    internalProperties.allowPlanFeatureGrants = false
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `returns no feature flags by default`() {
    userAccount = testData.pepa
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("enabledFeatureFlags").isArray.isEmpty()
    }
  }

  @Test
  fun `returns granted feature flags and plan features`() {
    organizationFeatureGrantService.setGrants(
      testData.pepaOrg.id,
      setOf(FeatureFlag.AI_REVIEW),
      setOf(Feature.GLOSSARY),
      null,
    )

    userAccount = testData.pepa
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("enabledFeatureFlags").isArray.containsExactly("AI_REVIEW")
      node("enabledFeatures").isArray.contains("GLOSSARY")
    }
  }

  @Test
  fun `returns feature flags enabled for all organizations`() {
    tolgeeProperties.featureFlags.enabledForAll = listOf(FeatureFlag.AI_REVIEW)

    userAccount = testData.pepa
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("enabledFeatureFlags").isArray.containsExactly("AI_REVIEW")
    }
  }

  @Test
  fun `grants of another organization are not exposed`() {
    organizationFeatureGrantService.setGrants(
      testData.jirinaOrg.id,
      setOf(FeatureFlag.AI_REVIEW),
      setOf(Feature.GLOSSARY),
      null,
    )

    userAccount = testData.pepa
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("enabledFeatureFlags").isArray.isEmpty()
      node("enabledFeatures").isArray.doesNotContain("GLOSSARY")
    }
  }
}
