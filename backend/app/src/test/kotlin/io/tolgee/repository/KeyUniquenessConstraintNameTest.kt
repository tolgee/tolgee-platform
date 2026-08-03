package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.SetKeysNamespaceChunkProcessor
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.key.Key
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * SetKeysNamespaceChunkProcessor recognises "this key name is taken" by the violated index name, so the
 * names it holds have to be the ones the database actually creates. The previous pair,
 * key_project_id_name_idx and key_project_id_name_namespace_id_idx, was dropped by changeSet
 * 1758202102054-2 while the guard went on matching them, which left it silently dead.
 *
 * These drive the guard itself against a real violation, so they fail if the index names, the driver's
 * ErrorResponse field, or Hibernate's wrapping stop lining up.
 */
@SpringBootTest
class KeyUniquenessConstraintNameTest : AbstractSpringTest() {
  @Autowired
  private lateinit var processor: SetKeysNamespaceChunkProcessor

  @Test
  @Transactional
  fun `a duplicate key outside a namespace reports the index the guard knows`() {
    val testData = BaseTestData()
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

    assertThat(processor.violatesKeyUniqueness(thrown!!))
      .describedAs("the guard must recognise a duplicate outside a namespace")
      .isTrue()
  }

  @Test
  @Transactional
  fun `a duplicate key inside a namespace reports the index the guard knows`() {
    val testData = BaseTestData()
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

    assertThat(processor.violatesKeyUniqueness(thrown!!))
      .describedAs("the guard must recognise a duplicate inside a namespace")
      .isTrue()
  }
}
