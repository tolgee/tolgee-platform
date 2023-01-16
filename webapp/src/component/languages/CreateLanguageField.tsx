import React, { FC, useEffect, useState } from 'react';
import { Box, BoxProps, Button } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';

import {
  AutocompleteOption,
  LanguageAutocomplete,
} from './LanguageAutocomplete';
import { LanguageModifyForm } from './LanguageModifyForm';
import { PreparedLanguage } from './PreparedLanguage';

export const CreateLanguageField: FC<{
  onSubmit?: (value) => void;
  value: components['schemas']['LanguageDto'] | null;
  onChange: (value: components['schemas']['LanguageDto'] | null) => void;
  onPreparedLanguageEdit?: () => void;
  showSubmitButton?: boolean;
  onPreparedClear?: () => void;
  preparedLanguageWrapperProps?: BoxProps;
  onAutocompleteClear?: () => void;
  onEditChange?: (edit: boolean) => void;
  modifyInDialog?: boolean;
  autoFocus?: boolean;
}> = (props) => {
  const [preferredEmojis, setPreferredEmojis] = useState([] as string[]);
  const [edit, setEdit] = useState(false);

  const { t } = useTranslate();

  useEffect(() => {
    props.onEditChange?.(edit);
  }, [edit]);

  const onSelectInAutocomplete = (option: AutocompleteOption) => {
    if (option) {
      props.onChange({
        name: option.englishName,
        originalName: option.originalName,
        tag: option.languageId,
        flagEmoji: option.flags?.[0] || '',
      });
      setPreferredEmojis(option.flags);
      if (option.isNew) {
        setEdit(true);
      }
    }
  };

  return (
    <>
      {props.value && (!edit || props.modifyInDialog) ? (
        <Box display="flex" justifyItems="center">
          <PreparedLanguage
            {...props.value}
            onReset={() => {
              if (props.onPreparedClear) {
                props.onPreparedClear?.();
              } else {
                props.onChange(null);
              }
            }}
            onEdit={() => {
              props.onPreparedLanguageEdit?.();
              setEdit(true);
            }}
          />
          {props.showSubmitButton && (
            <Box ml={1}>
              <Button
                data-cy="languages-create-submit-button"
                variant="contained"
                color="primary"
                onClick={() => {
                  props.onSubmit?.(props.value);
                }}
              >
                <T>language_create_add</T>
              </Button>
            </Box>
          )}
        </Box>
      ) : (
        !edit && (
          <Box flexGrow={1} mt={-2}>
            <LanguageAutocomplete
              autoFocus={props.autoFocus}
              onClear={props.onAutocompleteClear}
              onSelect={onSelectInAutocomplete}
            />
          </Box>
        )
      )}
      {edit && (
        <LanguageModifyForm
          inDialog={props.modifyInDialog}
          onModified={(value) => {
            props.onChange(value);
            setEdit(false);
          }}
          onCancel={() => {
            //don't submit invalid value in case of new custom language selection
            Validation.LANGUAGE(t)
              .validate(props.value)
              .then(() => {
                setEdit(false);
              })
              .catch(() => {
                setEdit(false);
                props.onChange(null);
              });
          }}
          values={props.value!}
          preferredEmojis={preferredEmojis}
        />
      )}
    </>
  );
};
