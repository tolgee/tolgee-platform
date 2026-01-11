package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityEntityDescribingPaths
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@ActivityLoggedEntity
@Table(
  indexes = [
    Index(columnList = "glossary_id"),
  ],
)
@ActivityEntityDescribingPaths(paths = ["glossary"])
class GlossaryTerm(
  @Column(columnDefinition = "text", nullable = false)
  @ActivityLoggedProp
  var description: String = "",
) : StandardAuditModel() {
  @ManyToOne
  lateinit var glossary: Glossary

  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], mappedBy = "term")
  var translations: MutableList<GlossaryTermTranslation> = mutableListOf()

  @ActivityLoggedProp
  var flagNonTranslatable: Boolean = false

  @ActivityLoggedProp
  var flagCaseSensitive: Boolean = false

  @ActivityLoggedProp
  var flagAbbreviation: Boolean = false

  @ActivityLoggedProp
  var flagForbiddenTerm: Boolean = false
}
