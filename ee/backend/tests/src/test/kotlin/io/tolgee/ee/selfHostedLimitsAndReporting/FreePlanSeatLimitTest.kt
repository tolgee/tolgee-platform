package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UserAccount
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest()
class FreePlanSeatLimitTest : AbstractSpringTest() {
  @Test
  fun `cannot create 11th user`() {
    createNUsers(9)

    // 10th user is fine
    userAccountService.createUser(
      UserAccount(
        username = "user10",
        password = "password",
      ),
    )

    val exception =
      assertThrows<BadRequestException> {
        userAccountService.createUser(
          UserAccount(
            username = "user11",
            password = "password",
          ),
        )
      }

    exception.tolgeeMessage.assert.isEqualTo(Message.FREE_SELF_HOSTED_SEAT_LIMIT_EXCEEDED)
  }

  @Test
  fun `can disable users when over plan`() {
    val users = createNUsers(20)
    getEnabledUsersCount().assert.isEqualTo(20L)
    userAccountService.disable(users[10].id)
    getEnabledUsersCount().assert.isEqualTo(19L)
  }

  @Test
  fun `can delete users when over plan`() {
    val users = createNUsers(20)
    getEnabledUsersCount().assert.isEqualTo(20L)
    userAccountService.delete(users[10].id)
    getEnabledUsersCount().assert.isEqualTo(19L)
  }

  private fun createNUsers(totalUserCount: Int): List<UserAccount> {
    val testData =
      BaseTestData()
    val users =
      (2..totalUserCount).map {
        testData.root.addUserAccount { username = "user$it" }.self
      }
    testDataService.saveTestData(testData.root)
    return users
  }

  private fun getEnabledUsersCount(): Long? {
    return entityManager
      .createQuery(
        "select count(*) from UserAccount ua where ua.disabledAt is null and ua.deletedAt is null",
        Long::class.java,
      ).singleResult
  }
}
