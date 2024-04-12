import { FC, useState } from 'react';
import { Box } from '@mui/material';
import { Alert } from '@mui/material';
import { isValidLanguageTag } from '@tginternal/language-util';
import { T } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { components } from 'tg.service/apiSchema.generated';

import { TextField } from '../common/form/fields/TextField';
import { FlagSelector } from './FlagSelector/FlagSelector';

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
      <Box
        display="grid"
        gridAutoFlow="column"
        gridTemplateColumns="100px auto"
        alignItems="start"
      >
        <Box mt={2}>
          <FlagSelector
            preferredEmojis={props.preferredEmojis || []}
            name="flagEmoji"
          />
        </Box>
        <Box ml={2}>
          <TextField
            variant="standard"
            fullWidth
            label={<T keyName="language_create_edit_abbreviation" />}
            name="tag"
            required={true}
            onValueChange={() => validateTag()}
          />
        </Box>
      </Box>
      <Box mb={4} minHeight={80}>
        {!tagValid && (
          <Alert severity="warning">
            <T keyName="invalid_language_tag" />
          </Alert>
        )}
      </Box>
    </>
  );
};
