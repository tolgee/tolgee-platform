package io.tolgee.dialects.postgres

import io.tolgee.model.UserAccount
import io.tolgee.testing.assertions.Assertions.assertThat
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class CustomPostgreSQLDialectTest {
  @Autowired
  lateinit var entityManager: EntityManager

  @Test
  fun `similarity function works`() {
    // Hibernate queries doesn't work without FROM clause, so we have
    // to create a dummy entity to select from
    entityManager.persist(UserAccount(username = "aaa", password = "aaaa", name = "aaaaa"))
    val query =
      entityManager.createQuery(
        "select similarity('I am so funny!', 'You are so funny!') from UserAccount",
      )
    assertThat(query.resultList).first().isEqualTo(0.47619048f)
  }
}
