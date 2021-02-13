package io.tolgee.exceptions;

import java.io.Serializable;
import java.util.List;

public class ErrorResponseBody {
    private String code;

    private List<Serializable> params;

    public ErrorResponseBody(String code, List<Serializable> params) {
        this.code = code;
        this.params = params;
    }

    public String getCode() {
        return this.code;
    }

    public List<Serializable> getParams() {
        return this.params;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setParams(List<Serializable> params) {
        this.params = params;
    }
}
