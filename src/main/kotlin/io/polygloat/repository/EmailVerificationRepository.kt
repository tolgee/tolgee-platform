package io.polygloat.repository

import io.polygloat.model.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailVerificationRepository : JpaRepository<EmailVerification, Long>