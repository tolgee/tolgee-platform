/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee

import io.tolgee.configuration.tolgee.ImportProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.Resource
import org.springframework.transaction.annotation.Transactional

@Suppress("LateinitVarOverridesLateinitVar")
@Transactional
class StartupImportCommandLineRunnerTest : AbstractSpringTest() {

  @Value("classpath:startup-import")
  lateinit var importDir: Resource

  @Autowired
  @SpyBean
  override lateinit var tolgeeProperties: TolgeeProperties

  @Autowired
  lateinit var startupImportCommandLineRunner: StartupImportCommandLineRunner

  @BeforeAll
  fun setup() {
    whenever(tolgeeProperties.import).thenReturn(
      ImportProperties().apply {
        dir = importDir.file.absolutePath
        createImplicitApiKey = true
      }
    )
  }

  @Test
  fun `imports data on startup`() {
    val user = dbPopulator.createUserIfNotExists("admin")
    startupImportCommandLineRunner.run()
    val projects = projectService.findAllByNameAndUserOwner("examples", user)
    assertThat(projects).isNotEmpty
    assertThat(projects[0].name).isEqualTo("examples")
    val languages = languageService.findAll(projects[0].id)
    assertThat(languageService.findAll(projects[0].id)).hasSize(4)
    languages.forEach {
      assertThat(translationService.getAllByLanguageId(it.id)).hasSize(10)
    }
    assertThat(projects[0].apiKeys.first().key).isEqualTo("examples-admin-imported-project-implicit")
  }
}
