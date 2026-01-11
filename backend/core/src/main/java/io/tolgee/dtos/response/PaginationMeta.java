package io.tolgee.dtos.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class PaginationMeta {
    private final int offset;
    private final Long allCount;

    @JsonCreator
    public PaginationMeta(@JsonProperty("offset") int offset, @JsonProperty("allCount") Long allCount) {
        this.offset = offset;
        this.allCount = allCount;
    }

    public int getOffset() {
        return this.offset;
    }

    public Long getAllCount() {
        return this.allCount;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof PaginationMeta)) return false;
        final PaginationMeta other = (PaginationMeta) o;
        if (this.getOffset() != other.getOffset()) return false;
        final Object this$allCount = this.getAllCount();
        final Object other$allCount = other.getAllCount();
        return this$allCount == null ? other$allCount == null : this$allCount.equals(other$allCount);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getOffset();
        final Object $allCount = this.getAllCount();
        result = result * PRIME + ($allCount == null ? 43 : $allCount.hashCode());
        return result;
    }

    public String toString() {
        return "PaginationMeta(offset=" + this.getOffset() + ", allCount=" + this.getAllCount() + ")";
    }
}
