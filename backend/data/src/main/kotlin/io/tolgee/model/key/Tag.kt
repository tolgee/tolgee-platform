package io.tolgee.model.key

import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OrderBy
import javax.validation.constraints.NotEmpty

@Entity
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

    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    return result
  }
}
