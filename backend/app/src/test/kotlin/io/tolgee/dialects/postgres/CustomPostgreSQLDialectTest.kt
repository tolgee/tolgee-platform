package io.tolgee.dialects.postgres

import io.tolgee.AbstractSpringTest
import io.tolgee.model.UserAccount
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class CustomPostgreSQLDialectTest : AbstractSpringTest() {

  @Test
  fun `similarity function works`() {
    // Hibernate queries doesn't work without FROM clause, so we have
    // to create a dummy entity to select from
    entityManager.persist(UserAccount(username = "aaa", password = "aaaa", name = "aaaaa"))
    val query = entityManager.createQuery(
      "select similarity('I am so funny!', 'You are so funny!') from UserAccount"
    )
    assertThat(query.resultList).first().isEqualTo(0.47619048f)
  }
}
