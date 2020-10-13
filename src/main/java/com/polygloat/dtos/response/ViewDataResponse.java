package com.polygloat.dtos.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ViewDataResponse<D, P> {
    PaginationMeta paginationMeta;
    P params;
    D data;

    public ViewDataResponse(D data, int offset, Long allCount, P params) {
        this.paginationMeta = new PaginationMeta(offset, allCount);
        this.params = params;
        this.data = data;
    }

    @JsonCreator()
    public ViewDataResponse(@JsonProperty("paginationMeta") PaginationMeta paginationMeta,
                            @JsonProperty("params") P params,
                            @JsonProperty("data") D data) {

        this.paginationMeta = paginationMeta;
        this.params = params;
        this.data = data;
    }

}
