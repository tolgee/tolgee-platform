package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.model.UserAccount
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@SpringBootTest
class UserAccountUsernameUniqueIndexTest : AbstractSpringTest() {
  @Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @Test
  @Transactional
  fun `rejects a second active account colliding on lower(username)`() {
    userAccountRepository.saveAndFlush(UserAccount(name = "a", username = "ciunique@test.com", password = "x"))

    assertThatThrownBy {
      userAccountRepository.saveAndFlush(UserAccount(name = "b", username = "CiUnique@test.com", password = "x"))
    }.isInstanceOf(DataIntegrityViolationException::class.java)
  }

  @Test
  @Transactional
  fun `rejects a disabled account colliding with an active one - disabled still reserves the email`() {
    userAccountRepository.saveAndFlush(UserAccount(name = "a", username = "ciunique2@test.com", password = "x"))

    assertThatThrownBy {
      userAccountRepository.saveAndFlush(
        UserAccount(name = "b", username = "CiUnique2@test.com", password = "x").apply {
          disabledAt = Date()
        },
      )
    }.isInstanceOf(DataIntegrityViolationException::class.java)
  }

  @Test
  @Transactional
  fun `allows a deleted account colliding on lower(username)`() {
    userAccountRepository.saveAndFlush(UserAccount(name = "a", username = "ciunique3@test.com", password = "x"))

    assertThatCode {
      userAccountRepository.saveAndFlush(
        UserAccount(name = "b", username = "CiUnique3@test.com", password = "x").apply {
          deletedAt = Date()
        },
      )
    }.doesNotThrowAnyException()
  }
}
