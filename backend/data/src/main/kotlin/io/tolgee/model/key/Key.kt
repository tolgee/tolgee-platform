package io.tolgee.model.key

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.dtos.PathDTO
import io.tolgee.events.OnKeyPrePersist
import io.tolgee.events.OnKeyPreRemove
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.StandardAuditModel
import io.tolgee.model.dataImport.WithKeyMeta
import io.tolgee.model.translation.Translation
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.context.ApplicationEventPublisher
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.OrderBy
import javax.persistence.PrePersist
import javax.persistence.PreRemove
import javax.persistence.Table
import javax.persistence.UniqueConstraint
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["project_id", "name"], name = "key_project_id_name")])
@ActivityLoggedEntity
@ActivityReturnsExistence
@EntityListeners(Key.Companion.KeyListeners::class)
class Key(
  @field:NotBlank
  @field:Size(max = 2000)
  @field:Column(length = 2000)
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String = "",
) : StandardAuditModel(), WithKeyMeta {
  @field:NotNull
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  lateinit var project: Project

  @OneToMany(mappedBy = "key")
  var translations: MutableSet<Translation> = mutableSetOf()

  @OneToOne(mappedBy = "key", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
  override var keyMeta: KeyMeta? = null

  @OneToMany(mappedBy = "key")
  @OrderBy("id")
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

  companion object {
    @Configurable
    class KeyListeners {
      @Autowired
      lateinit var eventPublisherProvider: ObjectFactory<ApplicationEventPublisher>

      @PrePersist
      fun prePersist(key: Key) {
        eventPublisherProvider.`object`.publishEvent(OnKeyPrePersist(source = this, key))
      }

      @PreRemove
      fun preRemove(key: Key) {
        eventPublisherProvider.`object`.publishEvent(OnKeyPreRemove(source = this, key))
      }
    }
  }
}
