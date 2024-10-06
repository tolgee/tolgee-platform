package io.tolgee.repository

import io.tolgee.model.EmailVerification
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface EmailVerificationRepository : JpaRepository<EmailVerification, Long>
