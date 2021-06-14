import React, { FunctionComponent, useEffect, useState } from 'react';
import { container } from 'tsyringe';
import { LanguageActions } from '../../store/languages/LanguageActions';
import { useProject } from '../../hooks/useProject';
import { components } from '../../service/apiSchema.generated';
import { Box, Button } from '@material-ui/core';
import { T } from '@tolgee/react';
import { LanguageModifyForm } from './LanguageModifyForm';
import { Validation } from '../../constants/GlobalValidationSchema';
import { ErrorResponseDto } from '../../service/response.types';
import { PreparedLanguage } from './PreparedLanguage';
import { ResourceErrorComponent } from '../common/form/ResourceErrorComponent';
import {
  AutocompleteOption,
  LanguageAutocomplete,
} from './LanguageAutocomplete';

const actions = container.resolve(LanguageActions);
export const LanguageCreate: FunctionComponent<{
  onCancel: () => void;
  onCreated?: (language: components['schemas']['LanguageModel']) => void;
}> = (props) => {
  const createLoadable = actions.useSelector((s) => s.loadables.create);
  const project = useProject();
  const [submitted, setSubmitted] = useState(false);

  const onSubmit = (values) => {
    setSubmitted(true);
    actions.loadableActions.create.dispatch({
      path: {
        projectId: project.id,
      },
      content: {
        'application/json': values,
      },
    });
  };

  useEffect(() => {
    if (createLoadable.loaded && submitted) {
      props.onCreated && props.onCreated(createLoadable.data!);
      setSubmitted(false);
      setValue(null);
    }
  }, [createLoadable.loading]);

  const [preferredEmojis, setPreferredEmojis] = useState([] as string[]);
  const [value, setValue] = useState(
    null as components['schemas']['LanguageDto'] | null
  );
  const [edit, setEdit] = useState(false);
  const [serverError, setServerError] = useState(
    undefined as ErrorResponseDto | undefined
  );

  const onSelectInAutocomplete = (option: AutocompleteOption) => {
    if (option) {
      setValue({
        name: option.englishName,
        originalName: option.originalName,
        tag: option.languageId,
        flagEmoji: option.flags?.[0] || '',
      });
      setPreferredEmojis(option.flags);
    }
    if (option.isNew) {
      setEdit(true);
    }
  };

  useEffect(() => {
    setServerError(createLoadable.error);
  }, [createLoadable.error]);

  return (
    <Box>
      {serverError && (
        <Box ml={2} mr={2}>
          <ResourceErrorComponent error={serverError} />
        </Box>
      )}
      {value && !edit ? (
        <Box display="flex">
          <PreparedLanguage
            {...value}
            onReset={() => {
              setServerError(undefined);
              setValue(null);
            }}
            onEdit={() => {
              setServerError(undefined);
              setEdit(true);
            }}
          />
          <Box ml={1}>
            <Button
              variant="contained"
              color="primary"
              onClick={() => {
                onSubmit(value);
              }}
            >
              <T>language_create_add</T>
            </Button>
          </Box>
        </Box>
      ) : !value ? (
        <Box flexGrow={1}>
          <LanguageAutocomplete onSelect={onSelectInAutocomplete} />
        </Box>
      ) : (
        <LanguageModifyForm
          onModified={(value) => {
            setValue(value);
            setEdit(false);
          }}
          onCancel={() => {
            //don't submit invalid value in case of new custom language selection
            Validation.LANGUAGE.validate(value)
              .then(() => {
                setEdit(false);
              })
              .catch(() => {
                setEdit(false);
                setValue(null);
              });
          }}
          values={value}
          preferredEmojis={preferredEmojis}
        />
      )}
    </Box>
  );
};
