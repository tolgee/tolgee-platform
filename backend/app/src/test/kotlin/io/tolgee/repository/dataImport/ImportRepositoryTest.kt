package io.tolgee.repository.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.model.dataImport.Import
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class ImportRepositoryTest : AbstractSpringTest() {

  @Autowired
  lateinit var importRepository: ImportRepository

  @Test
  fun `creates, saves and gets Import entity`() {
    val base = dbPopulator.createBase("hello", "importUser")
    Import(author = base.userOwner!!, project = base).let {
      importRepository.save(it).let {
        importRepository.getOne(it.id).let { got ->
          assertThat(got.author).isEqualTo(base.userOwner)
          assertThat(got.project).isEqualTo(base)
          assertThat(got.id).isGreaterThan(0L)
        }
      }
    }
  }
}
