package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@Transactional
class AllTranslationsServiceTest : AbstractSpringTest() {
  @Transactional
  @Test
  fun getTranslations() {
    val id = dbPopulator.populate("App").project.id
    val data =
      allTranslationsService.getAllTranslations(
        languageTags = HashSet(Arrays.asList("en", "de")),
        namespace = null,
        projectId = id,
        structureDelimiter = '.',
      )
    assertThat(data["en"]).isInstanceOf(MutableMap::class.java)
  }

  @Transactional
  @Test
  fun `returns correct map when collision`() {
    val project = dbPopulator.populate("App").project
    keyService.create(project, CreateKeyDto("folder.folder", null, mapOf("en" to "Ha")))
    keyService.create(project, CreateKeyDto("folder.folder.translation", null, mapOf("en" to "Ha")))

    val viewData =
      allTranslationsService.getAllTranslations(
        languageTags = HashSet(Arrays.asList("en", "de")),
        namespace = null,
        projectId = project.id,
        structureDelimiter = '.',
      )
    @Suppress("UNCHECKED_CAST")
    assertThat(viewData["en"] as Map<String, *>).containsKey("folder.folder.translation")
  }
}
