package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobService
import io.tolgee.config.BatchJobBaseConfiguration
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions
import java.util.function.Consumer

@Component
class BatchJobTestBase {
  lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobOperationQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var machineTranslationProperties: MachineTranslationProperties

  @Autowired
  lateinit var entityManager: EntityManager

  var fakeBefore: Boolean = false

  @Autowired
  private lateinit var internalProperties: InternalProperties

  @Autowired
  private lateinit var testDataService: TestDataService

  fun setup() {
    batchJobOperationQueue.clear()
    testData = BatchJobsTestData()

    whenever(internalProperties.fakeMtProviders).thenReturn(true)

    val googleMock = mock<GoogleMachineTranslationProperties>()
    whenever(googleMock.apiKey).thenReturn("mock")
    whenever(googleMock.defaultEnabled).thenReturn(true)
    whenever(googleMock.defaultPrimary).thenReturn(true)

    whenever(machineTranslationProperties.google).thenReturn(googleMock)

    val awsMock = mock<AwsMachineTranslationProperties>()
    whenever(awsMock.defaultEnabled).thenReturn(false)
    whenever(awsMock.accessKey).thenReturn("mock")
    whenever(awsMock.secretKey).thenReturn("mock")

    whenever(machineTranslationProperties.aws).thenReturn(awsMock)
  }

  fun saveAndPrepare(testClass: ProjectAuthControllerTest) {
    testDataService.saveTestData(testData.root)
    testClass.userAccount = testData.user
    testClass.projectSupplier = { testData.projectBuilder.self }
  }

  fun waitForAllTranslated(
    keyIds: List<Long>,
    keyCount: Int,
    expectedCsValue: String = "translated with GOOGLE from en to cs",
  ) {
    waitForNotThrowing(pollTime = 1000, timeout = 60000) {
      @Suppress("UNCHECKED_CAST")
      val czechTranslations =
        entityManager
          .createQuery(
            """
            from Translation t where t.key.id in :keyIds and t.language.tag = 'cs'
            """.trimIndent(),
          ).setParameter("keyIds", keyIds)
          .resultList as List<Translation>
      czechTranslations.assert.hasSize(keyCount)
      czechTranslations.forEach {
        it.text.assert.contains(expectedCsValue)
      }
    }
  }

  fun waitForJobCompleted(resultActions: ResultActions) =
    resultActions.andAssertThatJson {
      this.node("id").isNumber.satisfies(
        Consumer {
          waitFor(pollTime = 2000) {
            val job = batchJobService.findJobDto(it.toLong())
            job?.status?.completed == true
          }
        },
      )
    }
}
