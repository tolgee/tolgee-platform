package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema

class TranslationMemoryEntryCountsModel(
  @Schema(
    description =
      "Entry counts keyed by translation memory id. TM ids not visible to the caller " +
        "are omitted from the response.",
  )
  val counts: Map<Long, Long>,
)
