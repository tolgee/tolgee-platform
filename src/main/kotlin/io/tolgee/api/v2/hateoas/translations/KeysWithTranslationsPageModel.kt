package io.tolgee.api.v2.hateoas.translations

import io.tolgee.api.v2.hateoas.organization.LanguageModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.PagedModel

//They use the PagedModel(...) constructor themselves, so how could it be deprecated???
@Suppress("DEPRECATION", "unused")
class KeysWithTranslationsPageModel(
        content: Collection<KeyWithTranslationsModel>,
        metadata: PageMetadata?,
        vararg links: Link,
        val selectedLanguages: Collection<LanguageModel>
) : PagedModel<KeyWithTranslationsModel>(content, metadata, *links) {
}
