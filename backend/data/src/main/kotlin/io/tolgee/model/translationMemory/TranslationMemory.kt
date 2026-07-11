package io.tolgee.model.translationMemory

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.Organization
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Entity
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "organization_owner_id"),
    Index(columnList = "source_language_tag"),
  ],
)
class TranslationMemory(
  @field:Size(min = 1, max = 255)
  @ActivityLoggedProp
  var name: String = "",
  @Column(nullable = false)
  var sourceLanguageTag: String = "",
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  var type: TranslationMemoryType = TranslationMemoryType.PROJECT,
  @field:Min(0)
  @field:Max(100)
  @ActivityLoggedProp
  @Column(nullable = false)
  var defaultPenalty: Int = 0,
  @ActivityLoggedProp
  @Column(nullable = false)
  var writeOnlyReviewed: Boolean = false,
) : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  lateinit var organizationOwner: Organization

  // Note: no JPA cascade — entries and assignments are cleaned up by the DB-level
  // `ON DELETE CASCADE` on their foreign keys to `translation_memory`. Adding
  // `CascadeType.REMOVE` here would force Hibernate to load every child row before
  // issuing the delete, which is an unnecessary N+1.
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "translationMemory")
  var entries: MutableList<TranslationMemoryEntry> = mutableListOf()

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "translationMemory")
  var projectAssignments: MutableList<TranslationMemoryProject> = mutableListOf()
}
