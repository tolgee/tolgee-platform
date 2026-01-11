package io.tolgee.dtos.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ViewDataResponse<D, P> {
    private final PaginationMeta paginationMeta;
    private final P params;
    private final D data;

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

    public PaginationMeta getPaginationMeta() {
        return this.paginationMeta;
    }

    public P getParams() {
        return this.params;
    }

    public D getData() {
        return this.data;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ViewDataResponse)) return false;
        final ViewDataResponse<?, ?> other = (ViewDataResponse<?, ?>) o;
        final Object this$paginationMeta = this.getPaginationMeta();
        final Object other$paginationMeta = other.getPaginationMeta();
        if (this$paginationMeta == null ? other$paginationMeta != null : !this$paginationMeta.equals(other$paginationMeta)) return false;
        final Object this$params = this.getParams();
        final Object other$params = other.getParams();
        if (this$params == null ? other$params != null : !this$params.equals(other$params)) return false;
        final Object this$data = this.getData();
        final Object other$data = other.getData();
        return this$data == null ? other$data == null : this$data.equals(other$data);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $paginationMeta = this.getPaginationMeta();
        result = result * PRIME + ($paginationMeta == null ? 43 : $paginationMeta.hashCode());
        final Object $params = this.getParams();
        result = result * PRIME + ($params == null ? 43 : $params.hashCode());
        final Object $data = this.getData();
        result = result * PRIME + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    public String toString() {
        return "ViewDataResponse(paginationMeta=" + this.getPaginationMeta() + ", params=" + this.getParams() + ", data=" + this.getData() + ")";
    }
}
