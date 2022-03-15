package io.tolgee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.security.AuthenticationFacade
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@Transactional
class TranslationServiceTest : AbstractSpringTest() {

  @Autowired
  lateinit var authenticationFacade: AuthenticationFacade

  @Transactional
  @Test
  fun getTranslations() {
    val id = dbPopulator.populate("App").id
    val data = translationService.getTranslations(HashSet(Arrays.asList("en", "de")), id)
    assertThat(data["en"]).isInstanceOf(MutableMap::class.java)
  }

  @Transactional
  @Test
  fun `returns correct map when collision`() {
    val project = dbPopulator.populate("App")
    keyService.create(project, SetTranslationsWithKeyDto("folder.folder", mapOf("en" to "Ha")))
    keyService.create(project, SetTranslationsWithKeyDto("folder.folder.translation", mapOf("en" to "Ha")))

    val viewData = translationService.getTranslations(HashSet(Arrays.asList("en", "de")), project.id)
    @Suppress("UNCHECKED_CAST")
    assertThat(viewData["en"] as Map<String, *>).containsKey("folder.folder.translation")
  }
}
