package io.tolgee.dtos.response;

import io.tolgee.dtos.query_results.KeyDTO;

import java.util.LinkedHashMap;
import java.util.Map;

public class KeyResponseDTO {
    private Long id;

    private String name;

    private Map<String, String> translations = new LinkedHashMap<>();

    public KeyResponseDTO(Long id, String name, Map<String, String> translations) {
        this.id = id;
        this.name = name;
        this.translations = translations;
    }

    public KeyResponseDTO() {
    }

    public static KeyResponseDTO fromQueryResult(KeyDTO keyDTO) {
        return new KeyResponseDTO(keyDTO.getId(), keyDTO.getPath().getFullPathString(), keyDTO.getTranslations());
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Map<String, String> getTranslations() {
        return this.translations;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof KeyResponseDTO)) return false;
        final KeyResponseDTO other = (KeyResponseDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$translations = this.getTranslations();
        final Object other$translations = other.getTranslations();
        if (this$translations == null ? other$translations != null : !this$translations.equals(other$translations)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof KeyResponseDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $translations = this.getTranslations();
        result = result * PRIME + ($translations == null ? 43 : $translations.hashCode());
        return result;
    }

    public String toString() {
        return "KeyResponseDTO(id=" + this.getId() + ", name=" + this.getName() + ", translations=" + this.getTranslations() + ")";
    }
}
