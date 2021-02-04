package io.tolgee.dtos.request;

public class UaaGetKeyTranslations {
    private String key;

    public UaaGetKeyTranslations(String key) {
        this.key = key;
    }

    public UaaGetKeyTranslations() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof UaaGetKeyTranslations)) return false;
        final UaaGetKeyTranslations other = (UaaGetKeyTranslations) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof UaaGetKeyTranslations;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        return result;
    }

    public String toString() {
        return "UaaGetKeyTranslations(key=" + this.getKey() + ")";
    }
}
