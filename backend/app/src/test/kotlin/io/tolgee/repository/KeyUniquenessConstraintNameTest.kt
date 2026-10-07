package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.SetKeysNamespaceChunkProcessor
import io.tolgee.development.testDataBuilder.data.KeyUniquenessTestData
import io.tolgee.model.key.Key
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

class KeyUniquenessConstraintNameTest : AbstractSpringTest() {
  @Autowired
  private lateinit var processor: SetKeysNamespaceChunkProcessor

  private lateinit var testData: KeyUniquenessTestData

  @BeforeEach
  fun setup() {
    testData = KeyUniquenessTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @Transactional
  fun `the guard recognises a duplicate key outside a namespace`() {
    val thrown =
      catchThrowable {
        keyService.save(
          Key().apply {
            name = testData.keyWithoutNamespace.name
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
  fun `the guard recognises a duplicate key inside a namespace`() {
    val thrown =
      catchThrowable {
        keyService.save(
          Key().apply {
            name = testData.keyInNamespace.name
            project = testData.project
            namespace = testData.keyInNamespace.namespace
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
