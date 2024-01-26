package io.tolgee.ee

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.model.UserAccount
import io.tolgee.service.security.SignUpService
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.client.RestTemplate

@SpringBootTest()
class UserLimitTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @Autowired
  private lateinit var eeLicensingMockRequestUtil: EeLicensingMockRequestUtil

  @Autowired
  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  @Autowired
  private lateinit var signUpService: SignUpService

  @Test
  fun `cannot create 11th user`() {
    val testData =
      BaseTestData()
    (2..9).forEach {
      testData.root.addUserAccount { username = "user$it" }
    }
    testDataService.saveTestData(testData.root)

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

    exception as ExceptionWithMessage
    exception.tolgeeMessage.assert.isEqualTo(Message.FREE_SELF_HOSTED_SEAT_LIMIT_EXCEEDED)
  }
}
