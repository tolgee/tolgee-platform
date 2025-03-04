package io.tolgee.hateoas

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.PagedModel

open class PagedModelWithNextCursor<T>(
  pagedModel: PagedModel<T>,
  @field:Schema(
    description = "Cursor to get next data",
    example = "eyJrZXlJZCI6eyJkaXJlY3Rpb24iOiJBU0MiLCJ2YWx1ZSI6IjEwMDAwMDAxMjAifX0=",
  )
  val nextCursor: String?,
) : PagedModel<T>(pagedModel.content, pagedModel.metadata, pagedModel.links)
