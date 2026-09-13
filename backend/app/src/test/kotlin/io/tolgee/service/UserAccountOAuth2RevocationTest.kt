package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.UserAccountOAuth2RevocationTestData
import io.tolgee.development.testDataBuilder.newOAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserAccountOAuth2RevocationTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2GrantRepository

  private lateinit var testData: UserAccountOAuth2RevocationTestData

  @BeforeEach
  fun setup() {
    testData = UserAccountOAuth2RevocationTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `invalidating tokens deletes the grants, not just the JWT cutoff`() {
    val grantId = insertGrant()

    userAccountService.invalidateTokens(userAccountService.get(testData.subject.id))

    repository.existsById(grantId).assert.isFalse()
  }

  @Test
  fun `changing the password deletes the grants`() {
    val grantId = insertGrant()

    userAccountService.setUserPassword(userAccountService.get(testData.subject.id), "new-password")

    repository.existsById(grantId).assert.isFalse()
  }

  @Test
  fun `deleting the account deletes the grants, so nothing references the removed user`() {
    val grantId = insertGrant()

    userAccountService.delete(userAccountService.get(testData.subject.id))

    repository.existsById(grantId).assert.isFalse()
  }

  private fun insertGrant(): Long {
    val grant = newOAuth2Grant(testData.subject)
    repository.save(grant)
    return grant.id
  }
}
