package io.tolgee.model

import io.tolgee.model.key.Key
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
    Index(columnList = "key_id"),
    Index(columnList = "language_id"),
    Index(columnList = "user_id"),
  ],
)
class AiPlaygroundResult(
  @ManyToOne
  var project: Project,
  @ManyToOne
  var key: Key? = null,
  @ManyToOne
  var language: Language? = null,
  @ManyToOne
  var user: UserAccount? = null,
  @Column(columnDefinition = "text")
  var translation: String? = null,
  var contextDescription: String? = null,
) : StandardAuditModel()
