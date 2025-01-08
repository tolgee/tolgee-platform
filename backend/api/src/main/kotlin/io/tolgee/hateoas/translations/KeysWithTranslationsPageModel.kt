package io.tolgee.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.PagedModelWithNextCursor
import io.tolgee.hateoas.language.LanguageModel
import org.springframework.hateoas.Link

class KeysWithTranslationsPageModel(
  content: Collection<KeyWithTranslationsModel>,
  metadata: PageMetadata?,
  vararg links: Link,
  @Suppress("unused")
  @field:Schema(
    description = "Provided languages data",
  )
  val selectedLanguages: Collection<LanguageModel>,
  nextCursor: String?,
) : PagedModelWithNextCursor<KeyWithTranslationsModel>(content, metadata, links.toList(), nextCursor)
