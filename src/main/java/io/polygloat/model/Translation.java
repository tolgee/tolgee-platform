package io.polygloat.model;

import lombok.*;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"key_id", "language_id"}, name = "translation_key_language"),
})
@Data
public class Translation extends AuditModel {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(columnDefinition = "text")
    private String text;

    @ManyToOne
    private Key key;

    @ManyToOne
    private Language language;

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
}
