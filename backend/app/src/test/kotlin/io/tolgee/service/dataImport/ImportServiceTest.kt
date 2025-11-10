package io.tolgee.service.dataImport

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.dataImport.ImportNamespacesTestData
import io.tolgee.development.testDataBuilder.data.dataImport.ImportTestData
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class ImportServiceTest : AbstractSpringTest() {
  lateinit var importTestData: ImportTestData

  @BeforeEach
  fun setup() {
    importTestData = ImportTestData()
    importTestData.addFrenchTranslations()
  }

  @Test
  fun `it selects existing language`() {
    executeInNewTransaction {
      importTestData.setAllResolved()
      testDataService.saveTestData(importTestData.root)
    }
    executeInNewTransaction {
      val importFrench = importService.findLanguage(importTestData.importFrench.id)!!
      val french = languageService.getEntity(importTestData.french.id)
      importService.selectExistingLanguage(importFrench, french)
      assertThat(importFrench.existingLanguage).isEqualTo(french)
      val translations = importService.findTranslations(importTestData.importFrench.id)
      assertThat(translations.getByKey("what a key").conflict).isNotNull
      assertThat(translations.getByKey("what a beautiful key").conflict).isNull()
    }
  }

  fun Collection<ImportTranslation>.getByKey(key: String) = this.find { it.key.name == key } ?: error("Key not found")

  @Test
  fun `deletes import language`() {
    val testData =
      executeInNewTransaction {
        val testData = ImportTestData()
        testDataService.saveTestData(testData.root)
        assertThat(importService.findLanguage(testData.importEnglish.id)).isNotNull
        testData
      }
    executeInNewTransaction {
      importService.findLanguage(testData.importEnglish.id)?.let {
        importService.deleteLanguage(it)
      }
      entityManager.flush()
      entityManager.clear()
    }
    executeInNewTransaction {
      assertThat(importService.findLanguage(testData.importEnglish.id)).isNull()
    }
  }

  @Test
  fun `hard deletes import`() {
    val testData = ImportTestData()
    executeInNewTransaction {
      testData.addFileIssues()
      testData.addKeyMetadata()
      testDataService.saveTestData(testData.root)
    }

    executeInNewTransaction {
      val import = importService.get(testData.import.id)
      importService.hardDeleteImport(import)
    }
  }

  private fun checkImportHardDeleted(id: Long) {
    executeInNewTransaction {
      entityManager
        .createQuery("from Import i where i.id = :id", Import::class.java)
        .setParameter("id", id)
        .resultList
        .firstOrNull()
        .assert
        .isNull()
    }
  }

  @Test
  fun `soft deletes import`() {
    val testData = ImportTestData()
    executeInNewTransaction {
      testData.addFileIssues()
      testData.addKeyMetadata()
      testDataService.saveTestData(testData.root)
    }

    executeInNewTransaction {
      val import = importService.get(testData.import.id)
      importService.deleteImport(import)
    }

    executeInNewTransaction {
      assertThat(importService.find(testData.import.project.id, testData.import.author.id)).isNull()
    }

    waitForNotThrowing(pollTime = 200, timeout = 10000) {
      checkImportHardDeleted(testData.import.id)
    }
  }

  @Test
  fun `imports namespaces and merges same keys from multiple files`() {
    val testData =
      executeInNewTransaction {
        val testData = ImportNamespacesTestData()
        testDataService.saveTestData(testData.root)
        SecurityContextHolder.getContext().authentication =
          TolgeeAuthentication(
            credentials = null,
            deviceId = null,
            userAccount = UserAccountDto.fromEntity(testData.userAccount),
            actingAsUserAccount = null,
            isReadOnly = false,
            isSuperToken = false,
          )
        testData
      }
    executeInNewTransaction {
      permissionService.find(testData.project.id, testData.userAccount.id)
      val import = importService.get(testData.import.id)
      importService.import(import)
    }
    executeInNewTransaction {
      keyService.find(testData.project.id, "what a key", "homepage").assert.isNotNull
      val whatAKey = keyService.find(testData.project.id, "what a key", null)
      whatAKey!!
        .keyMeta!!
        .comments.assert
        .hasSize(2)
        .anyMatch { it.text == "hello1" }
        .anyMatch { it.text == "hello2" }
    }
  }
}
