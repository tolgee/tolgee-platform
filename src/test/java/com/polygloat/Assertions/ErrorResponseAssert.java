package com.polygloat.Assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.assertj.core.api.AbstractAssert;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class ErrorResponseAssert extends AbstractAssert<ErrorResponseAssert, MvcResult> {

    public ErrorResponseAssert(MvcResult mvcResult) {
        super(mvcResult, ErrorResponseAssert.class);
    }

    public StandardValidationMessageAssert isStandardValidation() {
        Map<String, String> standardValidation = getStandardMap().get("STANDARD_VALIDATION");
        if (standardValidation == null) {
            failWithMessage("Error response is not standard validation type.");
        }
        return new StandardValidationMessageAssert(standardValidation);
    }

    public CustomValidationMessageAssert isCustomValidation() {
        Map<String, List<Object>> customValidation = getCustomMap().get("CUSTOM_VALIDATION");
        if (customValidation == null) {
            failWithMessage("Error response is not custom validation type.");
        }
        return new CustomValidationMessageAssert(customValidation);
    }

    private Map<String, Map<String, String>> getStandardMap() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(actual.getResponse().getContentAsString(), new TypeReference<>() {
            });
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Can not parse error response.");
        }
    }

    @SneakyThrows
    private Map<String, Map<String, List<Object>>> getCustomMap() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(actual.getResponse().getContentAsString(), new TypeReference<>() {
            });
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Can not parse error response:\n" + actual.getResponse().getContentAsString());
        }
    }
}
