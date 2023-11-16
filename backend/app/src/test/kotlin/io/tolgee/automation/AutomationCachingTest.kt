package io.tolgee.automation

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Caches
import io.tolgee.development.testDataBuilder.data.CdnTestData
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.service.automations.AutomationService
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.cache.Cache
import javax.persistence.EntityManager

@AutoConfigureMockMvc
@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ]
)
class AutomationCachingTest : ProjectAuthControllerTest("/v2/projects/") {

  private lateinit var testData: CdnTestData

  @Autowired
  lateinit var automationService: AutomationService

  @Suppress("LateinitVarOverridesLateinitVar")
  @SpyBean
  @Autowired
  override lateinit var entityManager: EntityManager

  @BeforeEach
  fun before() {
    testData = CdnTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }
// práva - publish / manage
// push - last event
  // hlášky do zelena


  @Test
  @ProjectJWTAuthTestMethod
  fun `caches the automation`() {
    doGetAutomations()
    //first time - not cached
    val invocations = getEntityManagerInvocationsCount()

    //second time cached
    doGetAutomations()
    getEntityManagerInvocationsCount().assert.isEqualTo(invocations)
    getFromCache().assert.isNotNull
  }

  private fun getFromCache(): Cache.ValueWrapper? =
    cacheManager.getCache(Caches.AUTOMATIONS)!!.get(
      listOf(
        testData.projectBuilder.self.id,
        AutomationTriggerType.TRANSLATION_DATA_MODIFICATION,
        null
      )
    )

  @Test
  @ProjectJWTAuthTestMethod
  fun `save clears the cache`() {
    doGetAutomations()
    getFromCache().assert.isNotNull
    automationService.save(automationService.get(testData.automation.self.id))
    getFromCache().assert.isNull()
  }


  @Test
  @ProjectJWTAuthTestMethod
  fun `delete clears the cache`() {
    doGetAutomations()
    getFromCache().assert.isNotNull
    automationService.delete(testData.automation.self.id)
    getFromCache().assert.isNull()
  }

  private fun getEntityManagerInvocationsCount() = Mockito.mockingDetails(entityManager).invocations.size

  private fun doGetAutomations() {
    automationService.getProjectAutomations(
      testData.projectBuilder.self.id,
      AutomationTriggerType.TRANSLATION_DATA_MODIFICATION,
      null
    )
  }
}
