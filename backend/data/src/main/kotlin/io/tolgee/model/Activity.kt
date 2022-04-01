package io.tolgee.model

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.RevisionType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class Activity(
  @ManyToOne
  @Id
  val activityRevision: ActivityRevision,

  @Id
  val entityClass: String,

  @Id
  val entityId: Long
) : Serializable {

  @Type(type = "jsonb")
  var oldValues: Map<String, Any?>? = null

  @Type(type = "jsonb")
  lateinit var newValues: Map<String, Any?>

  @Enumerated
  lateinit var revisionType: RevisionType
}
