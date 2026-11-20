package io.tolgee.unit.branching

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class TranslationMergeFlagsTest {
  @Test
  fun `flags stay on the target when the text does not move`() {
    val target =
      Translation("same").apply {
        outdated = true
        auto = true
        mtProvider = MtServiceType.GOOGLE
      }
    val source = Translation("same")
    target.merge(source, null, BranchKeyMergeResolutionType.SOURCE)
    target.outdated.assert.isTrue()
    target.auto.assert.isTrue()
    target.mtProvider.assert.isEqualTo(MtServiceType.GOOGLE)
  }

  @Test
  fun `flags follow the source when its text moves in`() {
    val target = Translation("old").apply { outdated = true }
    val source =
      Translation("new").apply {
        auto = true
        mtProvider = MtServiceType.DEEPL
      }
    target.merge(source, null, BranchKeyMergeResolutionType.SOURCE)
    target.text.assert.isEqualTo("new")
    target.outdated.assert.isFalse()
    target.auto.assert.isTrue()
    target.mtProvider.assert.isEqualTo(MtServiceType.DEEPL)
  }

  private val english = Language().apply { tag = "en" }
  private val german = Language().apply { tag = "de" }
  private val french = Language().apply { tag = "fr" }
  private val project = Project().apply { baseLanguage = english }

  @Test
  fun `base text moving in marks non-moving non-empty siblings outdated`() {
    val target = key(en = "old", de = "Hallo", fr = "")
    target.merge(key(en = "new", de = "Hallo", fr = ""), null, BranchKeyMergeResolutionType.SOURCE)
    target
      .translation(english)
      .outdated.assert
      .isFalse()
    target
      .translation(german)
      .outdated.assert
      .isTrue()
    target
      .translation(french)
      .outdated.assert
      .isFalse()
  }

  @Test
  fun `sibling keeps its flag when the base text does not move`() {
    val target = key(en = "same", de = "Hallo", fr = "")
    target.merge(key(en = "same", de = "Hallo", fr = "Bonjour"), null, BranchKeyMergeResolutionType.SOURCE)
    target
      .translation(french)
      .text.assert
      .isEqualTo("Bonjour")
    target
      .translation(german)
      .outdated.assert
      .isFalse()
  }

  @Test
  fun `sibling keeps its flag when the target base text wins`() {
    val target = key(en = "target", de = "Hallo", fr = "")
    target.merge(key(en = "source", de = "Hallo", fr = ""), null, BranchKeyMergeResolutionType.TARGET)
    target
      .translation(english)
      .text.assert
      .isEqualTo("target")
    target
      .translation(german)
      .outdated.assert
      .isFalse()
  }

  private fun key(
    en: String,
    de: String,
    fr: String,
  ): Key {
    val key = Key("key", project)
    key.translations =
      mutableListOf(Translation(en, key, english), Translation(de, key, german), Translation(fr, key, french))
    return key
  }

  private fun Key.translation(language: Language) = translations.single { it.language.tag == language.tag }
}
