package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.*

@Entity
@ActivityLoggedEntity
class GlossaryTerm(
  @ActivityLoggedProp
  var description: String? = null,
) : StandardAuditModel() {
  @ManyToOne
  lateinit var glossary: Glossary

  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.REMOVE], mappedBy = "glossaryTerm")
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
