package io.tolgee.ee.repository.glossary

import io.tolgee.model.glossary.GlossaryTerm
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface GlossaryTermRepository : JpaRepository<GlossaryTerm, Long>
