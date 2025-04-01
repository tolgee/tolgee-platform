package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne

@Entity
@ActivityLoggedEntity
class GlossaryTermTranslation(
  var languageCode: String,
  @ActivityLoggedProp
  var text: String? = null,
) : StandardAuditModel() {
  @ManyToOne
  lateinit var glossaryTerm: GlossaryTerm // TODO: rename to term
}
