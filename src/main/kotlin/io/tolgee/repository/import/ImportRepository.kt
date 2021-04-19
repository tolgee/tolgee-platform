package io.tolgee.repository.import

import io.tolgee.model.import.Import
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportRepository : JpaRepository<Import, Long> {
    fun findByRepositoryIdAndAuthorId(repositoryId: Long, authorId: Long): Import?
}
