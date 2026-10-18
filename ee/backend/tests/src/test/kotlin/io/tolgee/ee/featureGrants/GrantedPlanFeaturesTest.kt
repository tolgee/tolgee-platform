package io.tolgee.ee.featureGrants

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.OrganizationFeatureGrantsTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.service.organization.OrganizationFeatureGrantChangeService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest
@AutoConfigureMockMvc
class GrantedPlanFeaturesTest : AuthorizedControllerTest() {
  class FeaturesChangedRecorder {
    val events = CopyOnWriteArrayList<OnOrganizationFeaturesChanged>()

    @EventListener
    fun onFeaturesChanged(event: OnOrganizationFeaturesChanged) {
      events.add(event)
    }
  }

  @TestConfiguration
  class RecorderConfiguration {
    @Bean
    fun featuresChangedRecorder() = FeaturesChangedRecorder()
  }

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var organizationFeatureGrantChangeService: OrganizationFeatureGrantChangeService

  @Autowired
  private lateinit var featuresChangedRecorder: FeaturesChangedRecorder

  private lateinit var testData: OrganizationFeatureGrantsTestData

  private val organizationId: Long
    get() = testData.organization.id

  private val otherOrganizationId: Long
    get() = testData.otherOrganization.id

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
    testData = OrganizationFeatureGrantsTestData()
    internalProperties.allowPlanFeatureGrants = true
  }

  @AfterEach
  fun cleanup() {
    internalProperties.allowPlanFeatureGrants = false
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    featuresChangedRecorder.events.clear()
  }

  @Test
  fun `enabled features are the plan features plus the granted ones`() {
    testData.addGrant(Feature.GLOSSARY)
    saveTestData()

    enabledFeaturesProvider
      .get(organizationId)
      .toList()
      .assert
      .containsExactlyInAnyOrder(Feature.TASKS, Feature.GLOSSARY)
    enabledFeaturesProvider
      .get(otherOrganizationId)
      .toList()
      .assert
      .containsExactly(Feature.TASKS)
    enabledFeaturesProvider
      .get(null)
      .toList()
      .assert
      .containsExactly(Feature.TASKS)
    enabledFeaturesProvider
      .getPlanFeatures(organizationId)
      .toList()
      .assert
      .containsExactly(Feature.TASKS)
  }

  @Test
  fun `missing hidden features give 404 and are left out of the FEATURE_NOT_ENABLED params`() {
    saveTestData()

    assertThrows<NotFoundException> {
      enabledFeaturesProvider.checkFeatureEnabled(organizationId, Feature.AI_REVIEW)
    }
    assertThrows<NotFoundException> {
      enabledFeaturesProvider.checkOneOfFeaturesEnabled(organizationId, listOf(Feature.AI_REVIEW))
    }
    val badRequest =
      assertThrows<BadRequestException> {
        enabledFeaturesProvider.checkOneOfFeaturesEnabled(
          organizationId,
          listOf(Feature.AI_REVIEW, Feature.GLOSSARY),
        )
      }
    badRequest.params.assert.containsExactly(Feature.GLOSSARY)
  }

  @Test
  fun `granted hidden feature passes the feature check`() {
    testData.addGrant(Feature.AI_REVIEW)
    saveTestData()

    enabledFeaturesProvider.checkFeatureEnabled(organizationId, Feature.AI_REVIEW)
  }

  @Test
  fun `granted hidden feature is not exposed to a user with limited view of the organization`() {
    testData.addGrant(Feature.AI_REVIEW)
    saveTestData()

    userAccount = testData.publicProjectOnlyViewer
    performAuthPut("/v2/user-preferences/set-preferred-organization/$organizationId", null).andIsOk
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("enabledFeatures").isArray.contains(Feature.TASKS.name).doesNotContain(Feature.AI_REVIEW.name)
    }

    userAccount = testData.user
    performAuthGet("/v2/preferred-organization").andIsOk.andAssertThatJson {
      node("enabledFeatures").isArray.contains(Feature.AI_REVIEW.name)
    }
  }

  @Test
  fun `grants do not unlock plan features on a self-hosted instance without billing`() {
    testData.addGrant(Feature.GLOSSARY)
    saveTestData()
    internalProperties.allowPlanFeatureGrants = false

    enabledFeaturesProvider
      .get(organizationId)
      .toList()
      .assert
      .containsExactly(Feature.TASKS)
    userAccount = testData.user
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsBadRequest
  }

  @Test
  fun `granted feature passes the feature check of an endpoint`() {
    saveTestData()
    userAccount = testData.user
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsBadRequest

    grant(otherOrganizationId, Feature.GLOSSARY)
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsBadRequest

    grant(organizationId, Feature.GLOSSARY)
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsOk
  }

  @Test
  fun `administration distinguishes plan features from granted ones`() {
    testData.addGrant(Feature.GLOSSARY)
    testData.addGrant(Feature.TASKS)
    saveTestData()
    userAccount = testData.admin

    performAuthGet("/v2/administration/organizations/$organizationId/features").andIsOk.andAssertThatJson {
      node("planFeatures[${Feature.TASKS.ordinal}].fromPlan").isEqualTo(true)
      node("planFeatures[${Feature.TASKS.ordinal}].granted").isEqualTo(true)
      node("planFeatures[${Feature.GLOSSARY.ordinal}].fromPlan").isEqualTo(false)
      node("planFeatures[${Feature.GLOSSARY.ordinal}].granted").isEqualTo(true)
      node("planFeatures[${Feature.QA_CHECKS.ordinal}].fromPlan").isEqualTo(false)
      node("planFeatures[${Feature.QA_CHECKS.ordinal}].granted").isEqualTo(false)
      node("planFeatures[${Feature.QA_CHECKS.ordinal}].hidden").isEqualTo(false)
      node("planFeatures[${Feature.AI_REVIEW.ordinal}].hidden").isEqualTo(true)
    }
  }

  @Test
  fun `changing grants publishes the effective feature change`() {
    saveTestData()
    grant(organizationId, Feature.GLOSSARY, Feature.TASKS)
    grant(organizationId, Feature.QA_CHECKS, Feature.TASKS)

    val events = featuresChangedRecorder.events
    events.map { it.organizationId }.assert.containsOnly(organizationId)
    events.map { it.gainedFeatures }.assert.containsExactly(setOf(Feature.GLOSSARY), setOf(Feature.QA_CHECKS))
    events.map { it.lostFeatures }.assert.containsExactly(emptySet(), setOf(Feature.GLOSSARY))
  }

  private fun grant(
    organizationId: Long,
    vararg features: Feature,
  ) {
    organizationFeatureGrantChangeService.setGrants(organizationId, features.toSet(), null)
  }
}
