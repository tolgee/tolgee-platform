/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee

import io.tolgee.commandLineRunners.StartupImportCommandLineRunner
import io.tolgee.configuration.tolgee.ImportProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.development.Base
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Suppress("LateinitVarOverridesLateinitVar")
@CleanDbBeforeClass
class StartupImportCommandLineRunnerTest : AbstractSpringTest() {
  private lateinit var base: Base

  @Value("classpath:startup-import")
  lateinit var importDir: Resource

  @Autowired
  override lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  lateinit var startupImportCommandLineRunner: StartupImportCommandLineRunner

  @BeforeAll
  fun setup() {
    tolgeeProperties.import =
      ImportProperties().apply {
        dir = importDir.file.absolutePath
        createImplicitApiKey = true
        baseLanguageTag = "de"
      }
    executeInNewTransaction {
      base = dbPopulator.createBase("admin")
      startupImportCommandLineRunner.run()
    }
  }

  @AfterAll
  fun tearDown() {
    tolgeeProperties.import = ImportProperties()
  }

  @Test
  fun `imports data on startup`() {
    executeInNewTransaction {
      val projects = projectService.findAllByNameAndOrganizationOwner("examples", base.organization)
      assertThat(projects).isNotEmpty
      val project = projects.first()
      val languages = languageService.findAll(project.id)
      assertThat(languageService.findAll(project.id)).hasSize(4)
      languages.forEach {
        assertThat(translationService.getAllByLanguageId(it.id)).hasSize(10)
      }
      assertThat(project.apiKeys.first().keyHash).isEqualTo("Zy98PdrKTEla1Ix7I1WbZPRoIDttk+Byk77tEjgRIzs=")
      assertThat(project.useNamespaces).isFalse()
    }
  }

  @Test
  fun `imports data with namespaces`() {
    executeInNewTransaction {
      val projects = projectService.findAllByNameAndOrganizationOwner("with-namespaces", base.organization)
      assertThat(projects).isNotEmpty
      val project = projects.first()
      project.namespaces.assert.hasSize(7)
      assertThat(project.useNamespaces).isTrue()
    }
  }

  @Test
  fun `sets base language`() {
    executeInNewTransaction {
      val projects = projectService.findAllByNameAndOrganizationOwner("examples", base.organization)
      assertThat(projects).isNotEmpty
      val project = projects.first()
      project.baseLanguage!!
        .tag.assert
        .isEqualTo("de")
      assertThat(project.useNamespaces).isFalse()
    }
  }
}
