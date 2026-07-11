package io.tolgee.model.key

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.validation.constraints.NotEmpty

@Entity
@Table(indexes = [Index(columnList = "project_id, name", unique = true)])
class Tag : StandardAuditModel() {
  @field:NotEmpty
  var name: String = ""

  @ManyToOne(fetch = FetchType.LAZY)
  var project: Project = Project()

  @ManyToMany(mappedBy = "tags")
  @OrderBy("id")
  var keyMetas: MutableSet<KeyMeta> = mutableSetOf()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as Tag

    return name == other.name
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode() + project.id.hashCode()
    return result
  }
}
