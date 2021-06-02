package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.assertions.Assertions.assertThatExceptionOfType
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.Test
import javax.validation.ConstraintViolationException

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

        val longName = StringBuilder().let { builder ->
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
        val base = dbPopulator.createBase("hello", "importUser")
        return importService.save(Import(author = base.userOwner!!, project = base))
    }
}
