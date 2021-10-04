package io.tolgee.model.key

import io.tolgee.dtos.PathDTO
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.WithKeyMeta
import io.tolgee.model.translation.Translation
import org.hibernate.envers.Audited
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "name"], name = "key_project_id_name")])
@Audited
data class Key(
  @field:NotBlank
  @field:Size(max = 200)
  @Column(length = 2000)
  var name: String = "",
) : StandardAuditModel(), WithKeyMeta {
  @field:NotNull
  @ManyToOne(optional = false)
  var project: Project? = null

  @OneToMany(mappedBy = "key")
  var translations: MutableSet<Translation> = HashSet()

  @OneToOne(mappedBy = "key")
  override var keyMeta: KeyMeta? = null

  @OneToMany(mappedBy = "key")
  var screenshots: MutableSet<Screenshot> = mutableSetOf()

  constructor(
    name: String,
    project: Project,
    translations: MutableSet<Translation> = HashSet()
  ) : this(name) {
    this.project = project
    this.translations = translations
  }

  val path: PathDTO
    get() = PathDTO.fromFullPath(name)
}
