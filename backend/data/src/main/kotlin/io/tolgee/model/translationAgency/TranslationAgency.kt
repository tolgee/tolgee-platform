package io.tolgee.model.translationAgency

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.EntityWithId
import io.tolgee.model.ModelWithAvatar
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Type

@Entity
@Table()
class TranslationAgency : StandardAuditModel(), ModelWithAvatar, EntityWithId {
  @field:Size(max = 255)
  @Column(length = 255)
  var name: String = ""

  @field:Size(max = 2000)
  @Column(length = 2000)
  var description: String? = null

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var services: MutableList<String> = mutableListOf()

  @field:Size(max = 255)
  @Column(length = 255)
  var url: String? = null

  override var avatarHash: String? = null
}
