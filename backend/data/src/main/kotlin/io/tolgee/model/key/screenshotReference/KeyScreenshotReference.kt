package io.tolgee.model.key.screenshotReference

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@IdClass(KeyScreenshotReferenceId::class)
class KeyScreenshotReference {
  @ManyToOne(optional = false)
  @Id
  lateinit var key: Key

  @ManyToOne(optional = false)
  @Id
  lateinit var screenshot: Screenshot

  @JdbcTypeCode(SqlTypes.JSON)
  var positions: MutableList<KeyInScreenshotPosition>? = mutableListOf()

  @Column(columnDefinition = "text", length = 5000)
  var originalText: String? = null
}
