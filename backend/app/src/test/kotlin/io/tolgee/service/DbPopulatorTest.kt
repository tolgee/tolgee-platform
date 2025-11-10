package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.DbPopulatorReal
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.security.ApiKeyService
import io.tolgee.testing.AbstractTransactionalTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
class DbPopulatorTest : AbstractTransactionalTest() {
  @Autowired
  lateinit var populator: DbPopulatorReal

  @Autowired
  lateinit var userAccountRepository: UserAccountRepository

  @Autowired
  lateinit var apiKeyService: ApiKeyService

  @Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  lateinit var userAccount: UserAccount

  @BeforeEach
  fun setup() {
    populator.autoPopulate()
    userAccount =
      userAccountRepository
        .findByUsername(tolgeeProperties.authentication.initialUsername)
        .orElseThrow { NotFoundException() }
  }

  @Test
  @Transactional
  fun createsUser() {
    Assertions.assertThat(userAccount.name).isEqualTo(tolgeeProperties.authentication.initialUsername)
  }

  @Test
  @Transactional
  fun createsApiKey() {
    val key = apiKeyService.getAllByUser(userAccount.id).stream().findFirst()
    Assertions.assertThat(key).isPresent
    Assertions.assertThat(key.get().key).isEqualTo(null)
  }
}
