package io.tolgee.ee.featureFlags

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.GlossaryTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.UserAccount
import io.tolgee.service.featureFlags.OrganizationFeatureGrantService
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
  private lateinit var organizationFeatureGrantService: OrganizationFeatureGrantService

  @Autowired
  private lateinit var featuresChangedRecorder: FeaturesChangedRecorder

  private lateinit var testData: GlossaryTestData

  private val organizationId: Long
    get() = testData.organization.id

  private val otherOrganizationId: Long
    get() =
      testData.root.data.userAccounts
        .first { it.self.username == "Unaffiliated" }
        .defaultOrganizationBuilder.self.id

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.TASKS)
    testData = GlossaryTestData()
    testDataService.saveTestData(testData.root)
    featuresChangedRecorder.events.clear()
    internalProperties.allowPlanFeatureGrants = true
  }

  @AfterEach
  fun cleanup() {
    internalProperties.allowPlanFeatureGrants = false
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `enabled features are the plan features plus the granted ones`() {
    grant(organizationId, Feature.GLOSSARY)

    enabledFeaturesProvider
      .get(
        organizationId,
      ).toList()
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
    organizationFeatureGrantService
      .withoutGrants { enabledFeaturesProvider.get(organizationId) }
      .toList()
      .assert
      .containsExactly(Feature.TASKS)
  }

  @Test
  fun `grants do not unlock plan features on a self-hosted instance without billing`() {
    grant(organizationId, Feature.GLOSSARY)
    internalProperties.allowPlanFeatureGrants = false

    enabledFeaturesProvider
      .get(organizationId)
      .toList()
      .assert
      .containsExactly(Feature.TASKS)
    userAccount = testData.userOwner
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsBadRequest
  }

  @Test
  fun `granted feature passes the feature check of an endpoint`() {
    userAccount = testData.userOwner
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsBadRequest

    grant(otherOrganizationId, Feature.GLOSSARY)
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsBadRequest

    grant(organizationId, Feature.GLOSSARY)
    performAuthGet("/v2/organizations/$organizationId/glossaries").andIsOk
  }

  @Test
  fun `administration distinguishes plan features from granted ones`() {
    grant(organizationId, Feature.GLOSSARY, Feature.TASKS)
    val admin = userAccountService.get(testData.userUnaffiliated.id)
    admin.role = UserAccount.Role.ADMIN
    userAccountService.save(admin)
    userAccount = admin

    performAuthGet("/v2/administration/organizations/$organizationId/features").andIsOk.andAssertThatJson {
      node("planFeatures[${Feature.TASKS.ordinal}].fromPlan").isEqualTo(true)
      node("planFeatures[${Feature.TASKS.ordinal}].granted").isEqualTo(true)
      node("planFeatures[${Feature.GLOSSARY.ordinal}].fromPlan").isEqualTo(false)
      node("planFeatures[${Feature.GLOSSARY.ordinal}].granted").isEqualTo(true)
      node("planFeatures[${Feature.QA_CHECKS.ordinal}].fromPlan").isEqualTo(false)
      node("planFeatures[${Feature.QA_CHECKS.ordinal}].granted").isEqualTo(false)
    }
  }

  @Test
  fun `changing grants publishes the effective feature change`() {
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
    organizationFeatureGrantService.setGrants(organizationId, emptySet(), features.toSet(), null)
  }
}
