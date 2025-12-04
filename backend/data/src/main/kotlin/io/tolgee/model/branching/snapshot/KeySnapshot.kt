package io.tolgee.model.branching.snapshot

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.branching.Branch
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Entity
@Table(
  name = "branch_key_snapshot",
  indexes = [
    Index(columnList = "branch_id"),
    Index(columnList = "project_id"),
    Index(columnList = "original_key_id"),
    Index(columnList = "branch_key_id"),
  ],
)
class KeySnapshot(
  @field:NotBlank
  @field:Column(length = 2000)
  var name: String = "",
  var namespace: String? = null,
  var isPlural: Boolean = false,
  var pluralArgName: String? = null,
  /**
   * Reference to the original key from which this snapshot was created.
   * Used to match BASE → FEATURE → MAIN.
   */
  @field:NotNull
  var originalKeyId: Long = 0,
  /**
   * Reference to the copied key residing in the branch this snapshot belongs to.
   * Enables comparing the current branch key data with the snapshot baseline.
   */
  @field:NotNull
  var branchKeyId: Long = 0,
) : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  lateinit var project: Project

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id")
  lateinit var branch: Branch

  @OneToMany(mappedBy = "keySnapshot", cascade = [CascadeType.ALL], orphanRemoval = true)
  var translations: MutableList<TranslationSnapshot> = mutableListOf()

  @OneToOne(mappedBy = "keySnapshot", cascade = [CascadeType.ALL], orphanRemoval = true)
  var keyMetaSnapshot: KeyMetaSnapshot? = null
}
