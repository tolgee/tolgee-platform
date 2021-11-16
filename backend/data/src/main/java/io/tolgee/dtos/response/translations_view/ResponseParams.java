package io.tolgee.dtos.response.translations_view;

import java.util.Set;

public class ResponseParams {
    private String search;
    private Set<String> languages;

    public ResponseParams(String search, Set<String> languages) {
        this.search = search;
        this.languages = languages;
    }

    public ResponseParams() {
    }

    public String getSearch() {
        return this.search;
    }

    public Set<String> getLanguages() {
        return this.languages;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public void setLanguages(Set<String> languages) {
        this.languages = languages;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ResponseParams)) return false;
        final ResponseParams other = (ResponseParams) o;
        if (!other.canEqual(this)) return false;
        final Object this$search = this.getSearch();
        final Object other$search = other.getSearch();
        if (this$search == null ? other$search != null : !this$search.equals(other$search)) return false;
        final Object this$languages = this.getLanguages();
        final Object other$languages = other.getLanguages();
        return this$languages == null ? other$languages == null : this$languages.equals(other$languages);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ResponseParams;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $search = this.getSearch();
        result = result * PRIME + ($search == null ? 43 : $search.hashCode());
        final Object $languages = this.getLanguages();
        result = result * PRIME + ($languages == null ? 43 : $languages.hashCode());
        return result;
    }

    public String toString() {
        return "ResponseParams(search=" + this.getSearch() + ", languages=" + this.getLanguages() + ")";
    }
}
