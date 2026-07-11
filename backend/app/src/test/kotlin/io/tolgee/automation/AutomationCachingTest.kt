package io.tolgee.automation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigTestData
import io.tolgee.dtos.cacheable.automations.AutomationDto
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.service.automations.AutomationService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.Cache
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class AutomationCachingTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: ContentDeliveryConfigTestData

  @Autowired
  lateinit var automationService: AutomationService

  @Suppress("LateinitVarOverridesLateinitVar")
  @MockitoSpyBean
  @Autowired
  override lateinit var entityManager: EntityManager

  @BeforeEach
  fun before() {
    testData = ContentDeliveryConfigTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
    cacheManager.getCache(Caches.AUTOMATIONS)!!.clear()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `caches the automation`() {
    val zeroInvocations = getEntityManagerInvocationsCount()
    getAutomations(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION, null)

    // first time - not cached
    val invocations = getEntityManagerInvocationsCount()
    zeroInvocations.assert.isLessThan(invocations)

    // second time cached
    getAutomations(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION, null)
    val secondInvocations = getEntityManagerInvocationsCount()
    secondInvocations.assert.isEqualTo(invocations)
    getFromCache(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION).assert.isNotNull
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `save clears the cache`() {
    getAutomations(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION, null)
    getFromCache(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION).assert.isNotNull
    executeInNewTransaction {
      automationService.save(automationService.get(testData.automation.self.id))
    }
    getFromCache(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `delete clears the cache`() {
    getAutomations(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION, null)
    getFromCache(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION).assert.isNotNull
    automationService.delete(testData.automation.self.id)
    getFromCache(AutomationTriggerType.TRANSLATION_DATA_MODIFICATION).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `correctly caches when when request for null activity type`() {
    getAutomations(
      AutomationTriggerType.TRANSLATION_DATA_MODIFICATION,
      ActivityType.BATCH_TAG_KEYS,
    ).assert.isNotEmpty
    automationService.delete(testData.automation.self.id)
    getAutomations(
      AutomationTriggerType.TRANSLATION_DATA_MODIFICATION,
      ActivityType.BATCH_TAG_KEYS,
    ).assert.isEmpty()
  }

  private fun getEntityManagerInvocationsCount() =
    Mockito
      .mockingDetails(entityManager)
      .invocations
      .count {
        it.method.name == "createQuery" && (it.arguments[0] as? String)?.contains("Automation") == true
      }

  private fun getAutomations(
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType?,
  ): List<AutomationDto> {
    return automationService.getProjectAutomations(
      testData.projectBuilder.self.id,
      automationTriggerType,
      activityType,
    )
  }

  private fun getFromCache(
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType? = null,
  ): Cache.ValueWrapper? =
    cacheManager.getCache(Caches.AUTOMATIONS)!!.get(
      arrayListOf(
        testData.projectBuilder.self.id,
        automationTriggerType,
        activityType,
      ),
    )
}
