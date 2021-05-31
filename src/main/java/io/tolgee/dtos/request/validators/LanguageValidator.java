package io.tolgee.dtos.request.validators;

import io.tolgee.constants.Message;
import io.tolgee.dtos.request.LanguageDTO;
import io.tolgee.dtos.request.validators.exceptions.ValidationException;
import io.tolgee.exceptions.NotFoundException;
import io.tolgee.model.Language;
import io.tolgee.model.Project;
import io.tolgee.service.LanguageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Optional;

@Component
public class LanguageValidator {
    private LanguageService languageService;

    @Autowired
    public LanguageValidator(LanguageService languageService) {
        this.languageService = languageService;
    }

    public void validateEdit(LanguageDTO dto) {
        LinkedHashSet<ValidationError> validationErrors = new LinkedHashSet<>();

        //handle edit validation
        Language language = languageService.findById(dto.getId()).orElseThrow(NotFoundException::new);

        Project project = language.getProject();

        if (!language.getName().equals(dto.getName())) {
            validateNameUniqueness(dto, project).ifPresent(validationErrors::add);
        }
        if (!language.getAbbreviation().equals(dto.getAbbreviation())) {
            validateAbbreviationUniqueness(dto, project).ifPresent(validationErrors::add);
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }

    public void validateCreate(LanguageDTO dto, Project project) {
        LinkedHashSet<ValidationError> validationErrors = new LinkedHashSet<>();

        //handle create validation
        validateAbbreviationUniqueness(dto, project).ifPresent(validationErrors::add);
        validateNameUniqueness(dto, project).ifPresent(validationErrors::add);

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }

    private Optional<ValidationError> validateNameUniqueness(LanguageDTO dto, Project project) {
        if (languageService.findByName(dto.getName(), project).isPresent()) {
            return Optional.of(new ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_NAME_EXISTS));
        }
        return Optional.empty();
    }

    private Optional<ValidationError> validateAbbreviationUniqueness(LanguageDTO dto, Project project) {
        if (languageService.findByAbbreviation(dto.getAbbreviation(), project).isPresent()) {
            return Optional.of(new ValidationError(ValidationErrorType.CUSTOM_VALIDATION, Message.LANGUAGE_ABBREVIATION_EXISTS));
        }
        return Optional.empty();
    }
}
