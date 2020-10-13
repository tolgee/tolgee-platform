package com.polygloat.service;

import com.polygloat.constants.Message;
import com.polygloat.dtos.request.SignUpDto;
import com.polygloat.dtos.request.UserUpdateRequestDTO;
import com.polygloat.dtos.request.validators.exceptions.ValidationException;
import com.polygloat.model.UserAccount;
import com.polygloat.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserAccountService {
    private UserAccountRepository userAccountRepository;

    @Autowired
    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public Optional<UserAccount> getByUserName(String username) {
        return userAccountRepository.findByUsername(username);
    }

    public Optional<UserAccount> get(Long id) {
        return userAccountRepository.findById(id);
    }

    public UserAccount createUser(UserAccount userAccount) {
        this.userAccountRepository.save(userAccount);
        return userAccount;
    }

    public UserAccount createUser(SignUpDto request) {
        String encodedPassword = encodePassword(request.getPassword());
        UserAccount account = UserAccount.builder()
                .name(request.getName())
                .username(request.getEmail())
                .password(encodedPassword).build();
        this.createUser(account);
        return account;
    }

    public UserAccount getImplicitUser() {
        final String username = "___implicit_user";
        return this.userAccountRepository.findByUsername(username).orElseGet(() -> {
            UserAccount account = UserAccount.builder().name("No auth user").username(username).role(UserAccount.Role.ADMIN).build();
            this.createUser(account);
            return account;
        });
    }

    public Optional<UserAccount> findByThirdParty(String type, String id) {
        return this.userAccountRepository.findByThirdPartyAuthTypeAndThirdPartyAuthId(type, id);
    }

    @Transactional
    public void setResetPasswordCode(UserAccount userAccount, String code) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        userAccount.setResetPasswordCode(bCryptPasswordEncoder.encode(code));
        userAccountRepository.save(userAccount);
    }

    @Transactional
    public void setUserPassword(UserAccount userAccount, String password) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        userAccount.setPassword(bCryptPasswordEncoder.encode(password));
        userAccountRepository.save(userAccount);
    }

    @Transactional
    public boolean isResetCodeValid(UserAccount userAccount, String code) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.matches(code, userAccount.getResetPasswordCode());
    }

    @Transactional
    public void removeResetCode(UserAccount userAccount) {
        userAccount.setResetPasswordCode(null);
    }

    private String encodePassword(String rawPassword) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    @Transactional
    public void update(UserAccount userAccount, UserUpdateRequestDTO dto) {
        if (!userAccount.getUsername().equals(dto.getEmail())) {
            this.getByUserName(dto.getEmail()).ifPresent(i -> {
                throw new ValidationException(Message.USERNAME_ALREADY_EXISTS);
            });

            userAccount.setUsername(dto.getEmail());
        }

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            userAccount.setPassword(encodePassword(dto.getPassword()));
        }

        userAccount.setName(dto.getName());
        userAccountRepository.save(userAccount);
    }

    public boolean isAnyUserAccount() {
        return userAccountRepository.count() > 0;
    }
}
