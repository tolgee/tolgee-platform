package io.tolgee.repository.import

import io.tolgee.AbstractSpringTest
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.model.import.Import
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test

@SpringBootTest
class ImportRepositoryTest : AbstractSpringTest() {

    @Autowired
    lateinit var importRepository: ImportRepository

    @Test
    fun `creates, saves and gets Import entity`() {
        val base = dbPopulator.createBase("hello", "importUser")
        Import(author = base.userOwner!!, repository = base).let {
            importRepository.save(it).let {
                importRepository.getOne(it.id).let { got ->
                    assertThat(got.author).isEqualTo(base.userOwner)
                    assertThat(got.repository).isEqualTo(base)
                    assertThat(got.id).isGreaterThan(0L)
                }
            }
        }
    }
}
