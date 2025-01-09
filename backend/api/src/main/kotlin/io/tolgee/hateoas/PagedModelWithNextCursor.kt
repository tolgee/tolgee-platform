package io.tolgee.hateoas

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.Link
import org.springframework.hateoas.Links
import org.springframework.hateoas.PagedModel

open class PagedModelWithNextCursor<T>(
  content: Collection<T> = emptyList(),
  metadata: PageMetadata? = null,
  links: Iterable<Link> = Links.NONE,
  @field:Schema(
    description = "Cursor to get next data",
    example = "eyJrZXlJZCI6eyJkaXJlY3Rpb24iOiJBU0MiLCJ2YWx1ZSI6IjEwMDAwMDAxMjAifX0=",
  )
  val nextCursor: String?,
) : PagedModel<T>(content, metadata, links)
