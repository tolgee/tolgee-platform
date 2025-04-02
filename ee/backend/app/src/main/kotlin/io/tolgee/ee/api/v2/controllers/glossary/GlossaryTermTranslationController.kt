package io.tolgee.ee.api.v2.controllers.glossary

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/organizations/{organizationId:[0-9]+}/glossaries/{id:[0-9]+}/terms/{termId:[0-9]+}/translations")
@Tag(name = "Glossary Term")
class GlossaryTermTranslationController
