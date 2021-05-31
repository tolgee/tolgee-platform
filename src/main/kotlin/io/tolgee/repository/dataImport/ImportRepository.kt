package io.tolgee.project.dataImport

import io.tolgee.model.dataImport.Import
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportRepository : JpaRepository<Import, Long> {
    fun findByRepositoryIdAndAuthorId(projectId: Long, authorId: Long): Import?

    fun findAllByRepositoryId(projectId: Long): List<Import>
}
