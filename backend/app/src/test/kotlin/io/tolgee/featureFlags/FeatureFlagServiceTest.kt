package io.tolgee.featureFlags

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Caches
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.dtos.cacheable.OrganizationFeatureGrantsDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.OrganizationFeatureGrantKind
import io.tolgee.repository.OrganizationFeatureGrantRepository
import io.tolgee.service.featureFlags.FeatureFlagService
import io.tolgee.service.featureFlags.OrganizationFeatureGrantService
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class FeatureFlagServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var featureFlagService: FeatureFlagService

  @Autowired
  private lateinit var organizationFeatureGrantService: OrganizationFeatureGrantService

  @Autowired
  private lateinit var organizationFeatureGrantRepository: OrganizationFeatureGrantRepository

  private lateinit var testData: OrganizationTestData

  @BeforeEach
  fun setup() {
    testData = OrganizationTestData()
    internalProperties.allowPlanFeatureGrants = true
  }

  @AfterEach
  fun cleanup() {
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    internalProperties.allowPlanFeatureGrants = false
    testDataService.cleanTestData(testData.root)
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    clearCaches()
  }

  @Test
  fun `flag is disabled by default`() {
    saveTestData()
    featureFlagService.isEnabled(testData.pepaOrg.id, FeatureFlag.AI_REVIEW).assert.isFalse()
    featureFlagService.getEnabledFlags(testData.pepaOrg.id).assert.isEmpty()
  }

  @Test
  fun `flag enabled for all organizations by the server configuration`() {
    saveTestData()
    tolgeeProperties.featureFlags.enabledForAll = listOf(FeatureFlag.AI_REVIEW)

    featureFlagService.isEnabled(testData.pepaOrg.id, FeatureFlag.AI_REVIEW).assert.isTrue()
    featureFlagService.isEnabled(testData.jirinaOrg.id, FeatureFlag.AI_REVIEW).assert.isTrue()
    featureFlagService.isEnabled(null, FeatureFlag.AI_REVIEW).assert.isTrue()
  }

  @Test
  fun `flag granted to one organization does not affect others`() {
    saveTestData()
    organizationFeatureGrantService.setGrants(
      testData.pepaOrg.id,
      setOf(FeatureFlag.AI_REVIEW),
      emptySet(),
      testData.pepa,
    )

    featureFlagService.isEnabled(testData.pepaOrg.id, FeatureFlag.AI_REVIEW).assert.isTrue()
    featureFlagService.isEnabled(testData.jirinaOrg.id, FeatureFlag.AI_REVIEW).assert.isFalse()
    featureFlagService.isEnabled(null, FeatureFlag.AI_REVIEW).assert.isFalse()
  }

  @Test
  fun `granted plan features are resolved per organization`() {
    saveTestData()
    organizationFeatureGrantService.setGrants(
      testData.pepaOrg.id,
      emptySet(),
      setOf(Feature.GLOSSARY),
      testData.pepa,
    )

    organizationFeatureGrantService.getGrantedPlanFeatures(testData.pepaOrg.id).assert.containsExactly(Feature.GLOSSARY)
    organizationFeatureGrantService.getGrantedPlanFeatures(testData.jirinaOrg.id).assert.isEmpty()
    organizationFeatureGrantService.getGrantedPlanFeatures(null).assert.isEmpty()
    organizationFeatureGrantService
      .withoutGrants { organizationFeatureGrantService.getGrantedPlanFeatures(testData.pepaOrg.id) }
      .assert
      .isEmpty()
  }

  @Test
  fun `setting grants replaces the previous ones`() {
    saveTestData()
    organizationFeatureGrantService.setGrants(
      testData.pepaOrg.id,
      setOf(FeatureFlag.AI_REVIEW),
      setOf(Feature.GLOSSARY, Feature.TASKS),
      testData.pepa,
    )
    organizationFeatureGrantService.setGrants(
      testData.pepaOrg.id,
      emptySet(),
      setOf(Feature.TASKS, Feature.QA_CHECKS),
      testData.pepa,
    )

    organizationFeatureGrantService
      .getGrants(testData.pepaOrg.id)
      .assert
      .isEqualTo(OrganizationFeatureGrantsDto(emptySet(), setOf(Feature.TASKS, Feature.QA_CHECKS)))
    organizationFeatureGrantRepository.findAllByOrganizationId(testData.pepaOrg.id).assert.hasSize(2)
  }

  @Test
  fun `unknown stored names are ignored`() {
    testData.pepaOrgBuilder.build {
      addFeatureGrant {
        kind = OrganizationFeatureGrantKind.FEATURE_FLAG
        name = "REMOVED_FLAG"
      }
      addFeatureGrant {
        kind = OrganizationFeatureGrantKind.PLAN_FEATURE
        name = "REMOVED_FEATURE"
      }
      addFeatureGrant {
        kind = OrganizationFeatureGrantKind.FEATURE_FLAG
        name = FeatureFlag.AI_REVIEW.name
      }
    }
    saveTestData()

    organizationFeatureGrantService
      .getGrants(testData.pepaOrg.id)
      .assert
      .isEqualTo(OrganizationFeatureGrantsDto(setOf(FeatureFlag.AI_REVIEW), emptySet()))
  }

  @Test
  fun `plan feature grants are ignored and rejected without billing or the internal switch`() {
    testData.pepaOrgBuilder.build {
      addFeatureGrant {
        kind = OrganizationFeatureGrantKind.PLAN_FEATURE
        name = Feature.GLOSSARY.name
      }
    }
    saveTestData()
    internalProperties.allowPlanFeatureGrants = false

    organizationFeatureGrantService.getGrantedPlanFeatures(testData.pepaOrg.id).assert.isEmpty()
    assertThrows<BadRequestException> {
      organizationFeatureGrantService.setGrants(
        testData.pepaOrg.id,
        emptySet(),
        setOf(Feature.TASKS),
        testData.pepa,
      )
    }.code.assert.isEqualTo(Message.PLAN_FEATURE_GRANTS_NOT_ALLOWED.code)
  }

  @Test
  fun `grants are cached and the cache is evicted when grants change`() {
    saveTestData()
    organizationFeatureGrantService.getGrants(testData.pepaOrg.id)
    getCachedGrants(testData.pepaOrg.id).assert.isEqualTo(OrganizationFeatureGrantsDto(emptySet(), emptySet()))

    organizationFeatureGrantService.setGrants(
      testData.pepaOrg.id,
      setOf(FeatureFlag.AI_REVIEW),
      emptySet(),
      testData.pepa,
    )

    getCachedGrants(testData.pepaOrg.id).assert.isNull()
    featureFlagService.isEnabled(testData.pepaOrg.id, FeatureFlag.AI_REVIEW).assert.isTrue()
    getCachedGrants(testData.pepaOrg.id)
      .assert
      .isEqualTo(OrganizationFeatureGrantsDto(setOf(FeatureFlag.AI_REVIEW), emptySet()))
  }

  private fun getCachedGrants(organizationId: Long): OrganizationFeatureGrantsDto? =
    cacheManager
      .getCache(Caches.ORGANIZATION_FEATURE_GRANTS)!!
      .get(organizationId)
      ?.get() as OrganizationFeatureGrantsDto?
}
