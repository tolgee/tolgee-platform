package com.polygloat.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonHelper {
    public static String asJsonString(Object obj) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String jsonContent = mapper.writeValueAsString(obj);
            return jsonContent;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
