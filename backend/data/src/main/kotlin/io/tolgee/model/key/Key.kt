package io.tolgee.model.key

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.dtos.PathDTO
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.WithKeyMeta
import io.tolgee.model.translation.Translation
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "name"], name = "key_project_id_name")])

@ActivityLoggedEntity
@ActivityReturnsExistence
class Key(
  @field:NotBlank
  @field:Size(max = 2000)
  @field:Column(length = 2000)
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String = "",
) : StandardAuditModel(), WithKeyMeta {
  @field:NotNull
  @ManyToOne(optional = false)
  lateinit var project: Project

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
