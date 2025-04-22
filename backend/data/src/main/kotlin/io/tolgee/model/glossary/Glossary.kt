package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.SoftDeletable
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import jakarta.validation.constraints.Size
import java.util.Date

// TODO: unique organizationOwner+name

@Entity
@ActivityLoggedEntity
class Glossary(
  @field:Size(min = 3, max = 50)
  @ActivityLoggedProp
  var name: String = "",
  @ActivityLoggedProp
  @Column(nullable = false)
  var baseLanguageTag: String? = null,
) : StandardAuditModel(), SoftDeletable {
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "glossary")
  var terms: MutableList<GlossaryTerm> = mutableListOf()

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  lateinit var organizationOwner: Organization

  @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
  @JoinTable(
    name = "glossary_project",
    joinColumns = [JoinColumn(name = "project_id")],
    inverseJoinColumns = [JoinColumn(name = "glossary_id")],
  )
  var assignedProjects: MutableSet<Project> = mutableSetOf()

  @Temporal(TemporalType.TIMESTAMP)
  override var deletedAt: Date? = null
}
