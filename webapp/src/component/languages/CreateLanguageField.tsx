import React, { FC, useState } from 'react';
import { Box, Button } from '@material-ui/core';
import { PreparedLanguage } from './PreparedLanguage';
import { T } from '@tolgee/react';
import {
  AutocompleteOption,
  LanguageAutocomplete,
} from './LanguageAutocomplete';
import { LanguageModifyForm } from './LanguageModifyForm';
import { Validation } from '../../constants/GlobalValidationSchema';
import { components } from '../../service/apiSchema.generated';

export const CreateLanguageField: FC<{
  onSubmit: (value) => void;
  value: components['schemas']['LanguageDto'] | null;
  onChange: (value: components['schemas']['LanguageDto'] | null) => void;
  onPreparedLanguageEdit: () => void;
  showSubmitButton?: boolean;
}> = ({
  onSubmit,
  value,
  onChange,
  onPreparedLanguageEdit,
  showSubmitButton,
}) => {
  const [preferredEmojis, setPreferredEmojis] = useState([] as string[]);
  const [edit, setEdit] = useState(false);

  const onSelectInAutocomplete = (option: AutocompleteOption) => {
    if (option) {
      onChange({
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

  return value && !edit ? (
    <Box display="flex">
      <PreparedLanguage
        {...value}
        onReset={() => {
          onChange(null);
        }}
        onEdit={() => {
          onPreparedLanguageEdit();
          setEdit(true);
        }}
      />
      {showSubmitButton && (
        <Box ml={1}>
          <Button
            data-cy="languages-create-submit-button"
            variant="contained"
            color="primary"
            onClick={() => {
              onSubmit(value);
            }}
          >
            <T>language_create_add</T>
          </Button>
        </Box>
      )}
    </Box>
  ) : !value ? (
    <Box flexGrow={1}>
      <LanguageAutocomplete onSelect={onSelectInAutocomplete} />
    </Box>
  ) : (
    <LanguageModifyForm
      onModified={(value) => {
        onChange(value);
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
            onChange(null);
          });
      }}
      values={value}
      preferredEmojis={preferredEmojis}
    />
  );
};
