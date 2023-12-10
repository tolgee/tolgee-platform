package io.tolgee.service

import io.tolgee.AbstractServerAppTest
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class OrganizationServiceTest : AbstractServerAppTest() {

  @Test
  fun `deletes organization with preferences`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    organizationService.delete(testData.jirinaOrg)
    entityManager.flush()

    println(organizationService.find(testData.jirinaOrg.id)?.preferredBy)
    assertThat(organizationService.find(testData.jirinaOrg.id)).isNull()
  }
}
