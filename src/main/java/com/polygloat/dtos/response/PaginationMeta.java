package com.polygloat.dtos.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PaginationMeta {
    private int offset;
    private Long allCount;

    @JsonCreator
    public PaginationMeta(@JsonProperty("offset") int offset, @JsonProperty("allCount") Long allCount) {
        this.offset = offset;
        this.allCount = allCount;
    }
}
