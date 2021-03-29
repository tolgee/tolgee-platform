package io.tolgee.repository

import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PermissionRepository : JpaRepository<Permission?, Long?> {
    fun findOneByRepositoryIdAndUserId(repositoryId: Long?, userId: Long?): Permission?
    fun getAllByRepositoryAndUserNotNull(repository: io.tolgee.model.Repository?): Set<Permission>

    @Query("from Permission p join Repository r on r = p.repository where p.user = ?1 order by r.name")
    fun findAllByUser(userAccount: UserAccount?): LinkedHashSet<Permission>

    @Modifying
    @Query("delete from Permission p where p.repository.id = :repositoryId")
    fun deleteAllByRepositoryId(repositoryId: Long?)
}
