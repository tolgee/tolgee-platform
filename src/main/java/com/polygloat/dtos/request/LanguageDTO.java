package com.polygloat.dtos.request;

import com.polygloat.model.Language;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LanguageDTO {
    @Getter
    private Long id;

    @Getter
    @Setter
    @NotBlank
    @Length(max = 100)
    private String name;

    @Getter
    @Setter
    @NotBlank
    @Length(max = 20)
    private String abbreviation;

    public LanguageDTO(@NotBlank String name, @NotBlank String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public static LanguageDTO fromEntity(Language language) {
        LanguageDTO languageDTO = new LanguageDTO(language.getName(), language.getAbbreviation());
        languageDTO.id = language.getId();
        return languageDTO;
    }
}
