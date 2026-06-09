package io.tolgee.repository

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.UserAccountUsernameUniqueIndexTestData
import io.tolgee.model.UserAccount
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
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

  @BeforeEach
  fun setup() {
    testDataService.saveTestData(UserAccountUsernameUniqueIndexTestData().root)
  }

  @Test
  @Transactional
  fun `rejects a second active account colliding on lower(username)`() {
    assertThatThrownBy {
      userAccountRepository.saveAndFlush(UserAccount(name = "b", username = "CiUnique@test.com", password = "x"))
    }.isInstanceOf(DataIntegrityViolationException::class.java)
  }

  @Test
  @Transactional
  fun `rejects a disabled account colliding with an active one - disabled still reserves the email`() {
    assertThatThrownBy {
      userAccountRepository.saveAndFlush(
        UserAccount(name = "b", username = "CiUnique@test.com", password = "x").apply {
          disabledAt = Date()
        },
      )
    }.isInstanceOf(DataIntegrityViolationException::class.java)
  }

  @Test
  @Transactional
  fun `allows a deleted account colliding on lower(username)`() {
    assertThatCode {
      userAccountRepository.saveAndFlush(
        UserAccount(name = "b", username = "CiUnique@test.com", password = "x").apply {
          deletedAt = Date()
        },
      )
    }.doesNotThrowAnyException()
  }
}
