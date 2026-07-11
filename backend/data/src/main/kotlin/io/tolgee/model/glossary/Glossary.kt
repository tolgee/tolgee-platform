package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityDescribingProp
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.activity.propChangesProvider.GlossaryAssignedProjectsPropChangesProvider
import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.hibernate.annotations.SQLRestriction

@Entity
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "base_language_tag"),
    Index(columnList = "organization_owner_id"),
  ],
)
class Glossary(
  @field:Size(min = 3, max = 50)
  @ActivityLoggedProp
  @ActivityDescribingProp
  var name: String = "",
  @ActivityLoggedProp
  @ActivityDescribingProp
  @Column(nullable = false)
  var baseLanguageTag: String = "",
) : StandardAuditModel() {
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], mappedBy = "glossary")
  var terms: MutableList<GlossaryTerm> = mutableListOf()

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  lateinit var organizationOwner: Organization

  @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
  @JoinTable(
    name = "glossary_project",
    joinColumns = [JoinColumn(name = "glossary_id")],
    inverseJoinColumns = [JoinColumn(name = "project_id")],
  )
  @SQLRestriction("deleted_at is null")
  @ActivityLoggedProp(GlossaryAssignedProjectsPropChangesProvider::class)
  var assignedProjects: MutableSet<Project> = mutableSetOf()
}
