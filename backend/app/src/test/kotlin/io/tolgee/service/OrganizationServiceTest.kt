package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class OrganizationServiceTest : AbstractSpringTest() {

  @Test
  fun `deletes organization with preferences`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    organizationService.delete(testData.jirinaOrg)
    entityManager.flush()
    assertThat(organizationService.find(testData.jirinaOrg.id)).isNull()
  }
}
