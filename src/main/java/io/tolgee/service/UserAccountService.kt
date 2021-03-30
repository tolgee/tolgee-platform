package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.SignUpDto
import io.tolgee.dtos.request.UserUpdateRequestDTO
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.model.UserAccount
import io.tolgee.repository.UserAccountRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
open class UserAccountService(private val userAccountRepository: UserAccountRepository) {
    open fun getByUserName(username: String?): Optional<UserAccount> {
        return userAccountRepository.findByUsername(username)
    }

    open operator fun get(id: Long): Optional<UserAccount?> {
        return userAccountRepository.findById(id)
    }

    open fun createUser(userAccount: UserAccount): UserAccount {
        userAccountRepository.save(userAccount)
        return userAccount
    }

    open fun createUser(request: SignUpDto): UserAccount {
        val encodedPassword = encodePassword(request.password!!)
        val account = UserAccount(name = request.name, username = request.email, password = encodedPassword)
        this.createUser(account)
        return account
    }

    open val implicitUser: UserAccount
        get() {
            val username = "___implicit_user"
            return userAccountRepository.findByUsername(username).orElseGet {
                val account = UserAccount(name = "No auth user", username = username, role = UserAccount.Role.ADMIN)
                this.createUser(account)
                account
            }
        }

    open fun findByThirdParty(type: String?, id: String?): Optional<UserAccount> {
        return userAccountRepository.findByThirdPartyAuthTypeAndThirdPartyAuthId(type!!, id!!)
    }

    @Transactional
    open fun setResetPasswordCode(userAccount: UserAccount, code: String?) {
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        userAccount.resetPasswordCode = bCryptPasswordEncoder.encode(code)
        userAccountRepository.save(userAccount)
    }

    @Transactional
    open fun setUserPassword(userAccount: UserAccount, password: String?) {
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        userAccount.password = bCryptPasswordEncoder.encode(password)
        userAccountRepository.save(userAccount)
    }

    @Transactional
    open fun isResetCodeValid(userAccount: UserAccount, code: String?): Boolean {
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        return bCryptPasswordEncoder.matches(code, userAccount.resetPasswordCode)
    }

    @Transactional
    open fun removeResetCode(userAccount: UserAccount) {
        userAccount.resetPasswordCode = null
    }

    open fun getAllInOrganization(organizationId: Long, pageable: Pageable, search: String?): Page<Array<Any>> {
        return userAccountRepository.getAllInOrganization(organizationId, pageable, search = search ?: "")
    }

    private fun encodePassword(rawPassword: String): String {
        val bCryptPasswordEncoder = BCryptPasswordEncoder()
        return bCryptPasswordEncoder.encode(rawPassword)
    }

    @Transactional
    open fun update(userAccount: UserAccount, dto: UserUpdateRequestDTO) {
        if (userAccount.username != dto.email) {
            getByUserName(dto.email).ifPresent { throw ValidationException(Message.USERNAME_ALREADY_EXISTS) }
            userAccount.username = dto.email
        }
        if (dto.password != null && !dto.password.isEmpty()) {
            userAccount.password = encodePassword(dto.password)
        }
        userAccount.name = dto.name
        userAccountRepository.save(userAccount)
    }

    open val isAnyUserAccount: Boolean
        get() = userAccountRepository.count() > 0
}
