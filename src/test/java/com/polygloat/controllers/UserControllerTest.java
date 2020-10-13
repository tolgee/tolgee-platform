package com.polygloat.controllers;

import com.polygloat.Assertions.StandardValidationMessageAssert;
import com.polygloat.dtos.request.UserUpdateRequestDTO;
import com.polygloat.model.UserAccount;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.testng.annotations.Test;

import java.util.Optional;

import static com.polygloat.Assertions.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest extends SignedInControllerTest implements ITest {

    @Test
    void updateUser() throws Exception {
        UserUpdateRequestDTO requestDTO = UserUpdateRequestDTO.builder().email("ben@ben.aa").password("super new password").name("Ben's new name").build();

        MvcResult mvcResult = performPost("/api/user", requestDTO).andExpect(status().isOk()).andReturn();

        Optional<UserAccount> fromDb = userAccountService.getByUserName(requestDTO.getEmail());
        assertThat(fromDb).isNotEmpty();

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        assertThat(bCryptPasswordEncoder.matches(requestDTO.getPassword(), fromDb.get().getPassword())).describedAs("Password is changed").isTrue();

        assertThat(fromDb.get().getName()).isEqualTo(requestDTO.getName());
    }

    @Test
    void updateUserValidation() throws Exception {
        UserUpdateRequestDTO requestDTO = UserUpdateRequestDTO.builder().email("ben@ben.aa").password("").name("").build();

        MvcResult mvcResult = performPost("/api/user", requestDTO).andExpect(status().isBadRequest()).andReturn();

        StandardValidationMessageAssert standardValidation = assertThat(mvcResult).error().isStandardValidation();
        standardValidation.onField("password");
        standardValidation.onField("name");

        requestDTO = UserUpdateRequestDTO.builder().email("ben@ben.aa").password("aksjhd  dasdsa").name("a").build();
        dbPopulator.createUser(requestDTO.getEmail());
        mvcResult = performPost("/api/user", requestDTO).andExpect(status().isBadRequest()).andReturn();
        assertThat(mvcResult).error().isCustomValidation().hasMessage("username_already_exists");
    }

}
