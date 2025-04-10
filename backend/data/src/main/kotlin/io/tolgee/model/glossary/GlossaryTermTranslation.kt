package io.tolgee.model.glossary

import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@ActivityLoggedEntity
@Table(
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["term_id", "languageCode"]),
  ],
)
class GlossaryTermTranslation(
  var languageCode: String,
  @Column(columnDefinition = "text")
  @ActivityLoggedProp
  var text: String? = null,
) : StandardAuditModel() {
  @ManyToOne
  lateinit var term: GlossaryTerm
}
