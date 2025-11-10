package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.OrganizationTestData
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@Transactional
@SpringBootTest(
  properties = [
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
    "spring.jpa.show-sql=true",
    "tolgee.machine-translation.free-credits-amount=100000",
  ],
)
class OrganizationServiceTest : AbstractSpringTest() {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var sessionFactory: SessionFactory

  @Test
  fun `deletes organization with preferences`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)
    organizationService.delete(testData.jirinaOrg)
    entityManager.flush()

    println(organizationService.find(testData.jirinaOrg.id)?.preferredBy)
    assertThat(organizationService.find(testData.jirinaOrg.id)).isNull()
  }

  @Test
  fun `fetches organization without mt bucket (tests the one-to-one lazy initialization)`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)

    val organization =
      assertSingleStatement {
        entityManager
          .createQuery("from Organization where id = :id", Organization::class.java)
          .setParameter("id", testData.jirinaOrg.id)
          .singleResult
      }

    assertSingleStatement {
      organization.mtCreditBucket
    }
  }

  @Test
  fun `mt bucket fetches only mt bucket (tests the one-to-one lazy initialization)`() {
    val testData = OrganizationTestData()
    testDataService.saveTestData(testData.root)

    executeInNewTransaction {
      mtCreditBucketService.consumeCredits(testData.jirinaOrg.id, 1)
    }

    entityManager.clear()

    executeInNewTransaction {
      val bucket =
        assertSingleStatement {
          entityManager
            .createQuery("from MtCreditBucket mb where mb.organization.id = :id", MtCreditBucket::class.java)
            .setParameter("id", testData.jirinaOrg.id)
            .singleResult
        }

      assertSingleStatement {
        bucket.organization
          ?.name.assert
          .isEqualTo(testData.jirinaOrg.name)
      }
    }
  }

  fun <T> assertSingleStatement(fn: () -> T): T {
    sessionFactory.statistics.clear()
    val result = fn()
    sessionFactory.statistics.prepareStatementCount.assert
      .isEqualTo(1)
    return result
  }
}
