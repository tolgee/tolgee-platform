package io.tolgee.model.key.screenshotReference

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type

@Entity
@IdClass(KeyScreenshotReferenceId::class)
@Table(
  indexes = [
    Index(columnList = "key_id"),
    Index(columnList = "screenshot_id"),
  ],
)
class KeyScreenshotReference {
  @ManyToOne(optional = false)
  @Id
  lateinit var key: Key

  @ManyToOne(optional = false)
  @Id
  lateinit var screenshot: Screenshot

  @Type(JsonBinaryType::class)
  @Column(columnDefinition = "jsonb")
  var positions: MutableList<KeyInScreenshotPosition>? = mutableListOf()

  @Column(columnDefinition = "text", length = 2000)
  var originalText: String? = null
}
