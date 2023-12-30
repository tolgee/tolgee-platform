package io.tolgee.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.language.LanguageModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.PagedModel

@Suppress("unused")
class KeysWithTranslationsPageModel(
  content: Collection<KeyWithTranslationsModel>,
  metadata: PageMetadata?,
  vararg links: Link,
  @field:Schema(
    description = "Provided languages data",
  )
  val selectedLanguages: Collection<LanguageModel>,
  @field:Schema(
    description = "Cursor to get next data",
    example = "eyJrZXlJZCI6eyJkaXJlY3Rpb24iOiJBU0MiLCJ2YWx1ZSI6IjEwMDAwMDAxMjAifX0=",
  )
  val nextCursor: String?,
) : PagedModel<KeyWithTranslationsModel>(content, metadata, links.toList())
