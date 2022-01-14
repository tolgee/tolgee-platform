package io.tolgee.dialects.postgres

import io.tolgee.model.UserAccount
import io.tolgee.testing.assertions.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests
import org.testng.annotations.Test
import javax.persistence.EntityManager

@SpringBootTest
class CustomPostgreSQLDialectTest : AbstractTransactionalTestNGSpringContextTests() {

  @Autowired
  lateinit var entityManager: EntityManager

  @Test
  fun `similarity function works`() {
    // Hibernate queries doesn't work without FROM clause, so we have
    // to create a dummy entity to select from
    entityManager.persist(UserAccount(username = "aaa", password = "aaaa", name = "aaaaa"))
    val query = entityManager.createQuery(
      "select similarity('I am so funny!', 'You are so funny!') from UserAccount"
    )
    assertThat(query.singleResult).isEqualTo(0.47619048f)
  }
}
