package io.polygloat.model;

import javax.persistence.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"key_id", "language_id"}, name = "translation_key_language"),
})
public class Translation extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text")
    private String text;

    @ManyToOne
    private Key key;

    @ManyToOne
    private Language language;

    public Translation(Long id, String text, Key key, Language language) {
        this.id = id;
        this.text = text;
        this.key = key;
        this.language = language;
    }

    public Translation() {
    }

    public static TranslationBuilder builder() {
        return new TranslationBuilder();
    }

    public Language getLanguage() {
        return language;
    }

    public Key getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String toString() {
        return "Translation(id=" + this.getId() + ", text=" + this.getText() + ", key=" + this.getKey() + ", language=" + this.getLanguage() + ")";
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Translation)) return false;
        final Translation other = (Translation) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        if (this$text == null ? other$text != null : !this$text.equals(other$text)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Translation;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        return result;
    }

    public static class TranslationBuilder {
        private Long id;
        private String text;
        private Key key;
        private Language language;

        TranslationBuilder() {
        }

        public Translation.TranslationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public Translation.TranslationBuilder text(String text) {
            this.text = text;
            return this;
        }

        public Translation.TranslationBuilder key(Key key) {
            this.key = key;
            return this;
        }

        public Translation.TranslationBuilder language(Language language) {
            this.language = language;
            return this;
        }

        public Translation build() {
            return new Translation(id, text, key, language);
        }

        public String toString() {
            return "Translation.TranslationBuilder(id=" + this.id + ", text=" + this.text + ", key=" + this.key + ", language=" + this.language + ")";
        }
    }
}
