package io.tolgee.dtos.request.validators;

import io.tolgee.dtos.request.AbstractRepositoryDTO;
import io.tolgee.dtos.request.CreateRepositoryDTO;
import io.tolgee.dtos.request.EditRepositoryDTO;
import io.tolgee.dtos.request.validators.annotations.RepositoryRequest;
import io.tolgee.model.Repository;
import io.tolgee.security.AuthenticationFacade;
import io.tolgee.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class RepositoryValidator implements ConstraintValidator<RepositoryRequest, Object> {
    private RepositoryService repositoryService;
    private AuthenticationFacade authenticationFacade;

    @Autowired
    public RepositoryValidator(RepositoryService repositoryService, AuthenticationFacade authenticationFacade) {
        super();
        this.repositoryService = repositoryService;
        this.authenticationFacade = authenticationFacade;
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object instanceof EditRepositoryDTO) {
            EditRepositoryDTO edit = (EditRepositoryDTO) object;
            Repository repository = repositoryService.getById(edit.getRepositoryId()).orElse(null);

            if (repository == null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("NOT_EXIST")
                        .addPropertyNode("repositoryId");
                return false;
            }

            if (!repository.getName().equals(edit.getName())) {
                if (!validateUniqueness(context, edit)) {
                    return false;
                }
            }
        }

        if (object instanceof CreateRepositoryDTO) {
            CreateRepositoryDTO create = (CreateRepositoryDTO) object;
            //noinspection RedundantIfStatement
            if (!validateUniqueness(context, create)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean validateUniqueness(ConstraintValidatorContext context, AbstractRepositoryDTO dto) {
        if (repositoryService.findByName(dto.getName(), authenticationFacade.getUserAccount()).isPresent()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("NAME_EXISTS")
                    .addPropertyNode("name").addConstraintViolation();
            return false;
        }
        return true;
    }

}
