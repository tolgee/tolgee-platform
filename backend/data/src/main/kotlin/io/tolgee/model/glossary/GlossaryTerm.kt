package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
@ActivityLoggedEntity
class GlossaryTerm(
  @ActivityLoggedProp
  var description: String? = null,
) : StandardAuditModel() {
  @ManyToOne
  lateinit var glossary: Glossary

  @ActivityLoggedProp
  var flagNonTranslatable: Boolean = false

  @ActivityLoggedProp
  var flagCaseSensitive: Boolean = false

  @ActivityLoggedProp
  var flagAbbreviation: Boolean = false

  @ActivityLoggedProp
  var flagForbiddenTerm: Boolean = false
}
