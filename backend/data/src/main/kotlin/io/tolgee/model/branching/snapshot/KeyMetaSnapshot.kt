package io.tolgee.model.branching.snapshot

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@Table(name = "branch_key_meta_snapshot")
class KeyMetaSnapshot(
  @Column(columnDefinition = "text")
  var description: String? = null,

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var custom: MutableMap<String, Any?>? = null
) : StandardAuditModel() {

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "key_snapshot_id")
  lateinit var keySnapshot: KeySnapshot
}
