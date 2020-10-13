package com.polygloat.controllers;

import com.polygloat.Assertions.UserApiAppAction;
import com.polygloat.constants.ApiScope;
import com.polygloat.dtos.response.ApiKeyDTO.ApiKeyDTO;
import com.polygloat.model.Repository;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class AbstractUserAppApiTest extends AbstractControllerTest {

    protected MvcResult performAction(UserApiAppAction action) {
        try {
            ResultActions resultActions = mvc.perform(action.getRequestBuilder());
            if (action.getExpectedStatus() != null) {
                resultActions = resultActions.andExpect(status().is(action.getExpectedStatus().value()));
            }
            return resultActions.andReturn();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected MvcResult performAction(UserApiAppAction.UserApiAppActionBuilder builder) {
        return performAction(builder.build());
    }

    protected UserApiAppAction.UserApiAppActionBuilder buildAction() {
        return UserApiAppAction.builder();
    }

    protected ApiKeyDTO createBaseWithApiKey(ApiScope... scopes) {
        Set<ApiScope> scopesSet = Set.of(scopes);

        if (scopesSet.isEmpty()) {
            scopesSet = Set.of(ApiScope.values());
        }

        Repository base = this.dbPopulator.createBase(generateUniqueString());
        return this.apiKeyService.createApiKey(base.getCreatedBy(), scopesSet, base);
    }

}
