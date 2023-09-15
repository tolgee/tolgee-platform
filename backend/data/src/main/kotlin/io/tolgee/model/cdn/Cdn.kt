package io.tolgee.model.cdn

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity()
@Table(
  uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "slug"])]
)
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class Cdn(
  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project,
) : StandardAuditModel() {
  var name: String = ""

  var slug: String = ""

  @Type(type = "jsonb")
  var exportParams: ExportParams = ExportParams()
}
