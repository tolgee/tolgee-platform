package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.processors.SetKeysNamespaceChunkProcessor
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.key.Key
import org.apache.commons.lang3.exception.ExceptionUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.hibernate.exception.ConstraintViolationException
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * SetKeysNamespaceChunkProcessor recognises "this key name is taken" by the violated index name, so the
 * names it holds have to be the ones the database actually creates. The previous pair,
 * key_project_id_name_idx and key_project_id_name_namespace_id_idx, was dropped by changeSet
 * 1758202102054-2 while the guard went on matching them, which left it silently dead.
 */
@SpringBootTest
class KeyUniquenessConstraintNameTest : AbstractSpringTest() {
  @Test
  @Transactional
  fun `a duplicate key without a namespace reports the index the guard knows`() {
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

    assertThat(violatedIndexNames(thrown))
      .describedAs("the database must report an index SetKeysNamespaceChunkProcessor recognises")
      .isNotEmpty()
      .anyMatch { it in SetKeysNamespaceChunkProcessor.KEY_UNIQUENESS_INDEXES }
  }

  private fun violatedIndexNames(thrown: Throwable?) =
    ExceptionUtils
      .getThrowableList(thrown)
      .filterIsInstance<ConstraintViolationException>()
      .mapNotNull { it.constraintName }
}
