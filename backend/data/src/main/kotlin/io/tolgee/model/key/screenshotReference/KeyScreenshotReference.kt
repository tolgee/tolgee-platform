package io.tolgee.model.key.screenshotReference

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.ManyToOne

@Entity
@IdClass(KeyScreenshotReferenceId::class)
@TypeDefs(
  value = [TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)]
)
class KeyScreenshotReference {
  @ManyToOne(optional = false)
  @Id
  lateinit var key: Key

  @ManyToOne(optional = false)
  @Id
  lateinit var screenshot: Screenshot

  @Type(type = "jsonb")
  var positions: MutableList<KeyInScreenshotPosition>? = mutableListOf()

  var originalText: String? = null
}
