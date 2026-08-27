package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.SetKeysNamespaceChunkProcessor
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.key.Key
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

class KeyUniquenessConstraintNameTest : AbstractSpringTest() {
  @Autowired
  private lateinit var processor: SetKeysNamespaceChunkProcessor

  private lateinit var testData: BaseTestData

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @Transactional
  fun `a duplicate key outside a namespace reports the index the guard knows`() {
    testData = BaseTestData()
    testData.projectBuilder.addKey { name = "duplicated" }
    testDataService.saveTestData(testData.root)

    val thrown =
      catchThrowable {
        keyService.save(
          Key().apply {
            name = "duplicated"
            project = testData.project
          },
        )
        entityManager.flush()
      }

    assertThat(thrown).describedAs("saving a duplicate key must violate the unique index").isNotNull()
    assertThat(processor.violatesKeyUniqueness(thrown!!))
      .describedAs("the guard must recognise a duplicate outside a namespace")
      .isTrue()
  }

  @Test
  @Transactional
  fun `a duplicate key inside a namespace reports the index the guard knows`() {
    testData = BaseTestData()
    testData.projectBuilder.addKey(keyName = "duplicated", namespace = "homepage")
    testDataService.saveTestData(testData.root)
    val existing = keyService.find(testData.project.id, "duplicated", "homepage")

    val thrown =
      catchThrowable {
        keyService.save(
          Key().apply {
            name = "duplicated"
            project = testData.project
            namespace = existing?.namespace
          },
        )
        entityManager.flush()
      }

    assertThat(thrown).describedAs("saving a duplicate key must violate the unique index").isNotNull()
    assertThat(processor.violatesKeyUniqueness(thrown!!))
      .describedAs("the guard must recognise a duplicate inside a namespace")
      .isTrue()
  }
}
