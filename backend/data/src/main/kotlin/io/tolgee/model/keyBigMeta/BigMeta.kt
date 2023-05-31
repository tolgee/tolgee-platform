package io.tolgee.model.keyBigMeta

import io.tolgee.model.AuditModel
import io.tolgee.model.Project
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Index
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "key1_id, key2_id", unique = true),
  ]
)
@IdClass(BigMetaId::class)
class BigMeta(
  @Id
  var key1Id: Long = 0,

  @Id
  var key2Id: Long = 0
) : AuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  var distance: Long = 10000

  var hits: Long = 1
}
