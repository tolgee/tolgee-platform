package io.tolgee.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import org.hibernate.annotations.ColumnDefault

@Embeddable
data class BranchMergeFilters(
  @Schema(description = "Offer only keys that already exist in the target. New keys are left for a later merge.")
  @Column(name = "existing_keys_only", nullable = false)
  @ColumnDefault("false")
  var existingKeysOnly: Boolean = false,
  @Schema(
    description =
      "Offer a key only when every translation that changed on it is in the Reviewed state. " +
        "Deletions are always offered. Empty translations are ignored.",
  )
  @Column(name = "reviewed_only", nullable = false)
  @ColumnDefault("false")
  var reviewedOnly: Boolean = false,
  @Schema(description = "Offer only keys carrying this tag.")
  @Column(name = "tag_filter")
  var tag: String? = null,
  @Schema(description = "Offer only keys not carrying this tag.")
  @Column(name = "without_tag_filter")
  var withoutTag: String? = null,
)
