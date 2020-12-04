package io.polygloat.dtos.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class SetTranslationsDTO {
    /**
     * Key full path is stored as name in entity
     */
    @NotNull
    @NotBlank
    private String key;

    /**
     * Map of language abbreviation -> text
     */
    private Map<String, String> translations;

    public SetTranslationsDTO(@NotNull @NotBlank String key, Map<String, String> translations) {
        this.key = key;
        this.translations = translations;
    }

    public SetTranslationsDTO() {
    }

    public static SetTranslationsDTOBuilder builder() {
        return new SetTranslationsDTOBuilder();
    }


    public String getKey() {
        return key;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setKey(@NotNull @NotBlank String key) {
        this.key = key;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SetTranslationsDTO)) return false;
        final SetTranslationsDTO other = (SetTranslationsDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        final Object this$translations = this.getTranslations();
        final Object other$translations = other.getTranslations();
        if (this$translations == null ? other$translations != null : !this$translations.equals(other$translations)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SetTranslationsDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        final Object $translations = this.getTranslations();
        result = result * PRIME + ($translations == null ? 43 : $translations.hashCode());
        return result;
    }

    public String toString() {
        return "SetTranslationsDTO(key=" + this.getKey() + ", translations=" + this.getTranslations() + ")";
    }

    public static class SetTranslationsDTOBuilder {
        private @NotNull @NotBlank String key;
        private Map<String, String> translations;

        SetTranslationsDTOBuilder() {
        }

        public SetTranslationsDTO.SetTranslationsDTOBuilder key(@NotNull @NotBlank String key) {
            this.key = key;
            return this;
        }

        public SetTranslationsDTO.SetTranslationsDTOBuilder translations(Map<String, String> translations) {
            this.translations = translations;
            return this;
        }

        public SetTranslationsDTO build() {
            return new SetTranslationsDTO(key, translations);
        }

        public String toString() {
            return "SetTranslationsDTO.SetTranslationsDTOBuilder(key=" + this.key + ", translations=" + this.translations + ")";
        }
    }
}
