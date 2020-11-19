package io.polygloat.model;

import lombok.*;

import javax.persistence.*;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"key_id", "language_id"}, name = "translation_source_language"),
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
    private Source key;

    @ManyToOne
    private Language language;

    public Language getLanguage() {
        return language;
    }

    public Source getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public void setKey(Source key) {
        this.key = key;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public void setText(String text) {
        this.text = text;
    }
}
