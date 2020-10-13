package com.polygloat.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

public class ErrorResponseBody {
    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private List<Serializable> params;

    public ErrorResponseBody(String code, List<Serializable> params) {
        this.code = code;
        this.params = params;
    }
}
