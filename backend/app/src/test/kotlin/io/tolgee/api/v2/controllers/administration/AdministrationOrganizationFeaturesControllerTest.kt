package io.tolgee.api.v2.controllers.administration

import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.AdministrationTestData
import io.tolgee.dtos.cacheable.OrganizationFeatureGrantsDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.repository.OrganizationFeatureGrantRepository
import io.tolgee.service.featureFlags.OrganizationFeatureGrantService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class AdministrationOrganizationFeaturesControllerTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var organizationFeatureGrantService: OrganizationFeatureGrantService

  @Autowired
  private lateinit var organizationFeatureGrantRepository: OrganizationFeatureGrantRepository

  private lateinit var testData: AdministrationTestData

  private val organizationId: Long
    get() = testData.adminBuilder.defaultOrganizationBuilder.self.id

  private val url: String
    get() = "/v2/administration/organizations/$organizationId/features"

  @BeforeEach
  fun createData() {
    testData = AdministrationTestData()
    testDataService.saveTestData(testData.root)
    internalProperties.allowPlanFeatureGrants = true
  }

  @AfterEach
  fun cleanUp() {
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    internalProperties.allowPlanFeatureGrants = false
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `admin gets all flags and plan features`() {
    tolgeeProperties.featureFlags.enabledForAll = listOf(FeatureFlag.AI_REVIEW)
    userAccount = testData.admin

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("featureFlags") {
        isArray.hasSize(FeatureFlag.entries.size)
      }
      node("featureFlags[0].flag").isEqualTo("AI_REVIEW")
      node("featureFlags[0].granted").isEqualTo(false)
      node("featureFlags[0].enabledForAll").isEqualTo(true)
      node("planFeatures") {
        isArray.hasSize(Feature.entries.size)
      }
      node("planFeatures[0].feature").isEqualTo(Feature.entries.first().name)
      node("planFeatures[0].granted").isEqualTo(false)
      node("planFeatureGrantsAllowed").isEqualTo(true)
    }
  }

  @Test
  fun `admin sets flags and granted plan features`() {
    userAccount = testData.admin

    performAuthPut(
      url,
      mapOf(
        "featureFlags" to listOf("AI_REVIEW"),
        "grantedFeatures" to listOf("GLOSSARY", "TASKS"),
      ),
    ).andIsOk.andAssertThatJson {
      node("featureFlags[0].granted").isEqualTo(true)
      node("featureFlags[0].enabledForAll").isEqualTo(false)
      node("planFeatures[${Feature.GLOSSARY.ordinal}].granted").isEqualTo(true)
      node("planFeatures[${Feature.TASKS.ordinal}].granted").isEqualTo(true)
      node("planFeatures[${Feature.QA_CHECKS.ordinal}].granted").isEqualTo(false)
    }

    organizationFeatureGrantService
      .getGrants(organizationId)
      .assert
      .isEqualTo(OrganizationFeatureGrantsDto(setOf(FeatureFlag.AI_REVIEW), setOf(Feature.GLOSSARY, Feature.TASKS)))
    organizationFeatureGrantRepository
      .findAllByOrganizationId(organizationId)
      .map { it.createdBy?.id }
      .assert
      .containsOnly(testData.admin.id)

    performAuthPut(url, mapOf("featureFlags" to emptyList<String>(), "grantedFeatures" to listOf("TASKS")))
      .andIsOk
      .andAssertThatJson {
        node("featureFlags[0].granted").isEqualTo(false)
        node("planFeatures[${Feature.GLOSSARY.ordinal}].granted").isEqualTo(false)
        node("planFeatures[${Feature.TASKS.ordinal}].granted").isEqualTo(true)
      }
  }

  @Test
  fun `plan features cannot be granted on an instance without billing`() {
    internalProperties.allowPlanFeatureGrants = false
    userAccount = testData.admin

    performAuthGet(url).andIsOk.andAssertThatJson {
      node("planFeatureGrantsAllowed").isEqualTo(false)
    }
    performAuthPut(url, mapOf("featureFlags" to listOf("AI_REVIEW"), "grantedFeatures" to listOf("GLOSSARY")))
      .andIsBadRequest
      .andHasErrorMessage(Message.PLAN_FEATURE_GRANTS_NOT_ALLOWED)
    performAuthPut(url, mapOf("featureFlags" to listOf("AI_REVIEW"), "grantedFeatures" to emptyList<String>()))
      .andIsOk
      .andAssertThatJson {
        node("featureFlags[0].granted").isEqualTo(true)
      }
  }

  @Test
  fun `supporter can read but cannot change`() {
    userAccount = testData.supporter

    performAuthGet(url).andIsOk
    performAuthPut(url, mapOf("featureFlags" to listOf("AI_REVIEW"))).andIsForbidden

    organizationFeatureGrantService
      .getGrants(organizationId)
      .featureFlags.assert
      .isEmpty()
  }

  @Test
  fun `regular user cannot read or change`() {
    userAccount = testData.user

    performAuthGet(url).andIsForbidden
    performAuthPut(url, mapOf("featureFlags" to listOf("AI_REVIEW"))).andIsForbidden

    organizationFeatureGrantService
      .getGrants(organizationId)
      .featureFlags.assert
      .isEmpty()
  }

  @Test
  fun `returns 404 for unknown organization`() {
    userAccount = testData.admin

    performAuthGet("/v2/administration/organizations/9999999999/features").andIsNotFound
    performAuthPut(
      "/v2/administration/organizations/9999999999/features",
      mapOf("featureFlags" to listOf("AI_REVIEW")),
    ).andIsNotFound
  }
}
