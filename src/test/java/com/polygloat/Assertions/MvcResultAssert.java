package com.polygloat.Assertions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AbstractAssert;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class MvcResultAssert extends AbstractAssert<MvcResultAssert, MvcResult> {

    public MvcResultAssert(MvcResult mvcResult) {
        super(mvcResult, MvcResultAssert.class);
    }

    public ErrorResponseAssert error() {
        return new ErrorResponseAssert(actual);
    }
}
