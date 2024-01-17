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
  const { values } =
    useFormikContext<components['schemas']['LanguageRequest']>();

  const validateTag = () => {
    const tagValue = (values as any)['tag'];
    setTimeout(() => setTagValid(!tagValue || isValidLanguageTag(tagValue)));
  };

  return (
    <>
      <TextField
        variant="standard"
        label={<T keyName="language_create_edit_english_name_label" />}
        name="name"
        required={true}
      />
      <TextField
        variant="standard"
        label={<T keyName="language_create_edit_original_name_label" />}
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
            label={<T keyName="language_create_edit_abbreviation" />}
            name="tag"
            required={true}
            onValueChange={() => validateTag()}
          />
          {!tagValid && (
            <Box mb={4}>
              <Alert severity="warning">
                <T keyName="invalid_language_tag" />
              </Alert>
            </Box>
          )}
        </Box>
      </Box>
    </>
  );
};
