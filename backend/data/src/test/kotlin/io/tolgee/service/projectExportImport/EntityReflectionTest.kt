package io.tolgee.service.projectExportImport

import io.tolgee.model.Language
import io.tolgee.model.annotation.DoNotExport
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import jakarta.persistence.ManyToMany
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EntityReflectionTest {
  @Test
  fun `ManyToOne is always owning`() {
    assertThat(EntityReflection.isOwningAssociation(Key::class.java, "project")).isTrue()
    assertThat(EntityReflection.isOwningAssociation(Language::class.java, "project")).isTrue()
  }

  @Test
  fun `composite-id ManyToOne ids are owning`() {
    assertThat(EntityReflection.isOwningAssociation(KeyScreenshotReference::class.java, "key")).isTrue()
    assertThat(EntityReflection.isOwningAssociation(KeyScreenshotReference::class.java, "screenshot")).isTrue()
  }

  @Test
  fun `ManyToMany without mappedBy is owning, the mappedBy inverse is not`() {
    assertThat(EntityReflection.isOwningAssociation(KeyMeta::class.java, "tags")).isTrue()
    assertThat(EntityReflection.isOwningAssociation(Tag::class.java, "keyMetas")).isFalse()
    assertThat(EntityReflection.isOwningAssociation(Translation::class.java, "labels")).isTrue()
    assertThat(EntityReflection.isOwningAssociation(Label::class.java, "translations")).isFalse()
  }

  @Test
  fun `OneToMany mappedBy inverse is not owning`() {
    assertThat(EntityReflection.isOwningAssociation(Key::class.java, "translations")).isFalse()
    assertThat(EntityReflection.isOwningAssociation(Language::class.java, "translations")).isFalse()
  }

  @Test
  fun `DoNotExport is detected only on annotated columns`() {
    assertThat(EntityReflection.isDoNotExport(Translation::class.java, "promptId")).isTrue()
    assertThat(EntityReflection.isDoNotExport(Translation::class.java, "text")).isFalse()
    assertThat(EntityReflection.isDoNotExport(Translation::class.java, "state")).isFalse()
  }

  @Test
  fun `readProperty reads through the Kotlin getter`() {
    val tag = Tag().apply { name = "needs-review" }
    assertThat(EntityReflection.readProperty(tag, "name")).isEqualTo("needs-review")
    assertThat(EntityReflection.readProperty(tag, "nonexistent")).isNull()
  }

  @Test
  fun `detects use-site-targeted annotations on the field and the getter`() {
    assertThat(EntityReflection.isDoNotExport(UseSiteFixture::class.java, "getterAnnotated")).isTrue()
    assertThat(EntityReflection.isDoNotExport(UseSiteFixture::class.java, "fieldAnnotated")).isTrue()
    assertThat(EntityReflection.isDoNotExport(UseSiteFixture::class.java, "plain")).isFalse()
    assertThat(EntityReflection.isOwningAssociation(UseSiteFixture::class.java, "owningViaGetter")).isTrue()
    assertThat(EntityReflection.isOwningAssociation(UseSiteFixture::class.java, "inverseViaGetter")).isFalse()
  }

  @Suppress("unused")
  private class UseSiteFixture {
    @get:DoNotExport
    var getterAnnotated: Long? = null

    @field:DoNotExport
    var fieldAnnotated: Long? = null

    var plain: Long? = null

    @get:ManyToMany
    val owningViaGetter: MutableSet<Any> = mutableSetOf()

    @get:ManyToMany(mappedBy = "x")
    val inverseViaGetter: MutableSet<Any> = mutableSetOf()
  }
}
