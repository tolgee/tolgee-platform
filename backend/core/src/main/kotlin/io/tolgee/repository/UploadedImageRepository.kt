package io.tolgee.repository

import io.tolgee.model.UploadedImage
import io.tolgee.model.UserAccount
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface UploadedImageRepository : JpaRepository<UploadedImage, Long> {
  fun countAllByUserAccount(userAccount: UserAccount): Long

  @EntityGraph(attributePaths = ["userAccount"])
  fun findAllByIdIn(filenames: Collection<Long>): List<UploadedImage>

  @Query("from UploadedImage where createdAt < :date")
  fun findAllOlder(date: Date): List<UploadedImage>
}
