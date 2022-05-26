import { FC, useState } from 'react';
import { Box } from '@mui/material';
import { Alert } from '@mui/material';
import { isValidLanguageTag } from '@tginternal/language-util';
import { T } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { components } from 'tg.service/apiSchema.generated';

import { TextField } from '../common/form/fields/TextField';
import { FlagSelector } from './FlagSelector';

export const LanguageModifyFields: FC<{
  preferredEmojis?: string[];
}> = (props) => {
  const [tagValid, setTagValid] = useState(true);
  const { values } = useFormikContext<components['schemas']['LanguageDto']>();

  const validateTag = () => {
    const tagValue = (values as any)['tag'];
    setTimeout(() => setTagValid(!tagValue || isValidLanguageTag(tagValue)));
  };

  return (
    <>
      <TextField
        variant="standard"
        label={<T>language_create_edit_english_name_label</T>}
        name="name"
        required={true}
      />
      <TextField
        variant="standard"
        label={<T>language_create_edit_original_name_label</T>}
        name="originalName"
        required={true}
      />
      <Box display="flex">
        <FlagSelector
          preferredEmojis={props.preferredEmojis || []}
          name="flagEmoji"
        />
        <Box flexGrow={1} ml={2}>
          <TextField
            variant="standard"
            fullWidth
            label={<T>language_create_edit_abbreviation</T>}
            name="tag"
            required={true}
            onValueChange={() => validateTag()}
          />
          {!tagValid && (
            <Box mb={4}>
              <Alert severity="warning">
                <T>invalid_language_tag</T>
              </Alert>
            </Box>
          )}
        </Box>
      </Box>
    </>
  );
};
