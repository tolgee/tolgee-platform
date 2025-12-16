package io.tolgee.model.branching.snapshot

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.enums.TranslationState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Type

@Entity
@Table(
  name = "branch_translation_snapshot",
  indexes = [
    Index(columnList = "key_snapshot_id"),
    Index(columnList = "language"),
  ],
)
class TranslationSnapshot(
  @field:NotBlank
  var language: String,
  @field:NotBlank
  @Column(columnDefinition = "text")
  var value: String,
  @Enumerated
  @ColumnDefault(value = "2")
  @ActivityLoggedProp
  var state: TranslationState = TranslationState.TRANSLATED,
) : StandardAuditModel() {
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var labels: MutableSet<String> = mutableSetOf()

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "key_snapshot_id")
  lateinit var keySnapshot: KeySnapshot
}
