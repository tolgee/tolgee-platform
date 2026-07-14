package io.tolgee.service.projectExportImport

import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.Language
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyComment
import io.tolgee.model.task.Task
import jakarta.persistence.ManyToOne
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EntityAssociationsTest {
  @Test
  fun `nullable type with no NOT-NULL annotation is droppable`() {
    assertThat(EntityAssociations.isDroppableSingularAssociation(Task::class.java, "agency")).isTrue()
  }

  @Test
  fun `bare ManyToOne on a non-null type is non-droppable`() {
    assertThat(EntityAssociations.isDroppableSingularAssociation(Language::class.java, "project")).isFalse()
  }

  @Test
  fun `ManyToOne optional=false on a nullable type is non-droppable`() {
    assertThat(EntityAssociations.isDroppableSingularAssociation(KeyComment::class.java, "author")).isFalse()
  }

  @Test
  fun `ManyToOne optional=false on a non-null type is non-droppable`() {
    assertThat(EntityAssociations.isDroppableSingularAssociation(Key::class.java, "project")).isFalse()
  }

  @Test
  fun `JoinColumn(nullable=false) on a nullable type is non-droppable`() {
    assertThat(
      EntityAssociations.isDroppableSingularAssociation(TranslationSuggestion::class.java, "language"),
    ).isFalse()
  }

  @Test
  fun `OneToOne optional=false on a nullable type is non-droppable`() {
    assertThat(
      EntityAssociations.isDroppableSingularAssociation(AuthProviderChangeRequest::class.java, "userAccount"),
    ).isFalse()
  }

  @Test
  fun `reads a getter-targeted annotation when the backing field is unannotated`() {
    assertThat(EntityAssociations.isDroppableSingularAssociation(GetterAnnotatedFixture::class.java, "ref")).isFalse()
  }

  @Test
  fun `unresolvable property is non-droppable (fail-safe)`() {
    assertThat(EntityAssociations.isDroppableSingularAssociation(Key::class.java, "noSuchProperty")).isFalse()
  }

  private class GetterAnnotatedFixture {
    @get:ManyToOne(optional = false)
    val ref: Key? = null
  }
}
