package io.tolgee.model.translation

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.annotation.ActivityReturnsExistence
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Entity
@Table(indexes = [Index(columnList = "project_id, name", unique = true)])
@ActivityLoggedEntity
@ActivityReturnsExistence
class Label : StandardAuditModel() {
  @field:NotEmpty
  @field:Size(max = 100)
  @Column(length = 100, nullable = false)
  @ActivityLoggedProp
  @ActivityDescribingProp
  lateinit var name: String

  @field:NotEmpty
  @field:Size(max = 7)
  @Column(length = 7, nullable = false)
  @ActivityLoggedProp
  lateinit var color: String

  @field:Size(max = 2000)
  @Column(length = 2000)
  @ActivityLoggedProp
  var description: String? = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  lateinit var project: Project

  @ManyToMany(mappedBy = "labels")
  var translations: MutableSet<Translation> = mutableSetOf()

  fun clearTranslations() {
    translations.forEach {
      it.labels.remove(this)
    }
    translations.clear()
  }
}
