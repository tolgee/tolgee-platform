package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.testing.assertions.Assertions.assertThatExceptionOfType
import jakarta.validation.ConstraintViolationException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@Transactional
@SpringBootTest
class ImportFileRepositoryTest : AbstractSpringTest() {
  @Autowired
  lateinit var importFileRepository: ImportFileRepository

  @Test
  fun `creates and saves and gets ImportFile entity`() {
    val import = createBaseImport()

    ImportFile(import = import, name = "en.json").let {
      importFileRepository.save(it).let { saved ->
        importFileRepository.getOne(saved.id).let { got ->
          assertThat(got.name).isEqualTo(it.name)
          assertThat(got.import).isEqualTo(import)
          assertThat(got.id).isGreaterThan(0L)
        }
      }
    }
  }

  @Test
  fun `validates ImportFile entity`() {
    val import = createBaseImport()

    val longName =
      StringBuilder().let { builder ->
        repeat((1..2010).count()) {
          builder.append("a")
        }
        builder.toString()
      }

    ImportFile(import = import, name = longName).let {
      assertThatExceptionOfType(ConstraintViolationException::class.java)
        .isThrownBy {
          importFileRepository.save(it)
          entityManager.flush()
        }
    }
  }

  private fun createBaseImport(): Import {
    val base = dbPopulator.createBase("import-user")
    return importService.save(Import(project = base.project).also { it.author = base.userAccount })
  }
}
