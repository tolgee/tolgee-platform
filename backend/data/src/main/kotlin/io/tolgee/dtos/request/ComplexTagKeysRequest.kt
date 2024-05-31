package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

class ComplexTagKeysRequest(
  @Schema(description = "Include keys filtered by the provided key information")
  val filterKeys: List<KeyId>?,
  @Schema(description = "Exclude keys filtered by the provided key information")
  val filterKeysNot: List<KeyId>?,
  @Schema(
    description =
      "Include keys filtered by the provided tag information. " +
        "This filter supports wildcards. " +
        "For example, `draft-*` will match all tags starting with `draft-`.",
  )
  val filterTag: List<String>?,
  @Schema(
    description =
      "Exclude keys filtered by the provided tag information. " +
        "This filter supports wildcards. " +
        "For example, `draft-*` will match all tags starting with `draft-`.",
  )
  val filterTagNot: List<String>?,
  @Schema(description = "Specified tags will be added to filtered keys")
  val tagFiltered: List<String>?,
  @Schema(
    description =
      "Specified tags will be removed from filtered keys. " +
        "It supports wildcards. For example, `draft-*` will remove all tags starting with `draft-`.",
  )
  val untagFiltered: List<String>?,
  @Schema(description = "Specified tags will be added to keys not filtered by any of the specified filters.")
  val tagOther: List<String>?,
  @Schema(
    description =
      "Specified tags will be removed from keys not filtered by any of the specified filters. " +
        "It supports wildcards. For example, `draft-*` will remove all tags starting with `draft-`.",
  )
  val untagOther: List<String>?,
)
