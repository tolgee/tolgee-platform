package com.polygloat.model;

import com.polygloat.dtos.request.LanguageDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repository_id", "name"}, name = "language_repository_name"),
        @UniqueConstraint(columnNames = {"repository_id", "abbreviation"}, name = "language_abbreviation_name")
},
        indexes = {
                @Index(columnList = "abbreviation", name = "index_abbreviation"),
                @Index(columnList = "abbreviation, repository_id", name = "index_abbreviation_source")
        }
)
@ToString(of = {"id", "abbreviation", "name"})
public class Language extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private Long id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "language")
    @Getter
    @Setter
    private Set<Translation> translations;

    @ManyToOne
    @Getter
    @Setter
    private Repository repository;

    @Getter
    @Setter
    private String abbreviation;

    @Getter
    @Setter
    private String name;

    public static Language fromRequestDTO(LanguageDTO dto) {
        Language language = new Language();
        language.setName(dto.getName());
        language.setAbbreviation(dto.getAbbreviation());
        return language;
    }

    public void updateByDTO(LanguageDTO dto) {
        this.name = dto.getName();
        this.abbreviation = dto.getAbbreviation();
    }
}
