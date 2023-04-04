package io.tolgee.model.keyBigMeta

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Index
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "key_name, namespace"),
  ]
)
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class BigMeta : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY)
  lateinit var project: Project

  var namespace: String? = null

  @Column(name = "key_name")
  lateinit var keyName: String

  var location: String? = null

  @Type(type = "jsonb")
  var contextData: List<SurroundingKey>? = null
}
