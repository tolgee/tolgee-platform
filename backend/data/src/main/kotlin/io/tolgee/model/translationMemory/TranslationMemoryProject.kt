package io.tolgee.model.translationMemory

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

@Entity
@Table(
  uniqueConstraints = [
    UniqueConstraint(
      name = "UK_tm_project_tm_id_project_id",
      columnNames = ["translation_memory_id", "project_id"],
    ),
  ],
  indexes = [
    Index(columnList = "translation_memory_id"),
    Index(columnList = "project_id"),
  ],
)
class TranslationMemoryProject : StandardAuditModel() {
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  lateinit var translationMemory: TranslationMemory

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  lateinit var project: Project

  @Column(nullable = false)
  var readAccess: Boolean = true

  @Column(nullable = false)
  var writeAccess: Boolean = true

  /**
   * Ordering in suggestion results. Lower number = higher priority. The project's own TM is
   * created at 0 so shared TMs stack under it by default; users can drag to reorder.
   */
  @Column(nullable = false)
  var priority: Int = 0

  /**
   * Per-assignment penalty override. When null, the TM's [TranslationMemory.defaultPenalty]
   * applies. Penalty subtracts percentage points from the match score before display and
   * ranking, capped at 0.
   */
  @field:Min(0)
  @field:Max(100)
  @Column(nullable = true)
  var penalty: Int? = null
}
