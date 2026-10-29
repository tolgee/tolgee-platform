package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.dataImport.SingleStepImportBranchTestData
import io.tolgee.service.key.KeyMetaService
import io.tolgee.testing.assert
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
    "spring.jpa.show-sql=true",
    // keep in sync with OrganizationServiceTest properties to share Spring test context
    "tolgee.machine-translation.free-credits-amount=100000",
  ],
)
class KeyMetaServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var keyMetaService: KeyMetaService

  @Autowired
  private lateinit var sessionFactory: SessionFactory

  lateinit var testData: SingleStepImportBranchTestData

  @BeforeEach
  fun setup() {
    testData = SingleStepImportBranchTestData()
    testDataService.saveTestData(testData.root)
    // TestDataService does not persist key meta comments
    executeInNewTransaction {
      entityManager
        .createNativeQuery(
          """
          INSERT INTO key_comment (id, key_meta_id, author_id, text, from_import, created_at, updated_at)
          SELECT nextval('hibernate_sequence'), km.id, :authorId, 'comment of ' || k.name, false, now(), now()
          FROM key_meta km JOIN key k ON k.id = km.key_id
          WHERE k.project_id = :projectId AND k.name LIKE '%meta key%'
          """,
        ).setParameter("authorId", testData.user.id)
        .setParameter("projectId", testData.project.id)
        .executeUpdate()
    }
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `fetches branch key metas with comments and code references in two queries`() {
    executeInNewTransaction {
      val project = projectService.get(testData.project.id)
      sessionFactory.statistics.clear()

      val metas = keyMetaService.getWithFetchedData(project, testData.defaultBranch.name)
      val read =
        metas.associate { meta ->
          meta.key!!.name to
            listOf(
              meta.key!!.namespace?.name,
              meta.comments.joinToString { it.text },
              meta.codeReferences.joinToString { it.path },
            )
        }

      sessionFactory.statistics.prepareStatementCount.assert
        .isEqualTo(2)
      read.assert.isEqualTo(
        mapOf(
          "shared" to listOf(null, "", ""),
          "meta key 1" to listOf("ns1", "comment of meta key 1", "src/meta key 1.swift"),
          "meta key 2" to listOf("ns2", "comment of meta key 2", "src/meta key 2.swift"),
          "meta key 3" to listOf(null, "comment of meta key 3", "src/meta key 3.swift"),
        ),
      )
    }
  }
}
