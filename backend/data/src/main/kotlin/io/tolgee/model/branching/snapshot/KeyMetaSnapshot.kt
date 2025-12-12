package io.tolgee.model.branching.snapshot

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.key.Tag
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@Table(name = "branch_key_meta_snapshot")
class KeyMetaSnapshot(
  @Column(columnDefinition = "text")
  var description: String? = null,
  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var custom: MutableMap<String, Any?>? = null,
  @ManyToMany(cascade = [CascadeType.ALL])
  @OrderBy("id")
  var tags: MutableSet<Tag> = mutableSetOf(),
) : StandardAuditModel() {
  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "key_snapshot_id")
  lateinit var keySnapshot: KeySnapshot
}
