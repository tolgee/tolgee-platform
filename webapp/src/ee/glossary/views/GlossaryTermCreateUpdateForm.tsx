import { VFC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Checkbox } from 'tg.component/common/form/fields/Checkbox';
import Box from '@mui/material/Box';
import { useTranslate } from '@tolgee/react';
import { FormControlLabel, styled } from '@mui/material';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    'flagNonTranslatable flagCaseSensitive'
    'flagAbbreviation    flagForbiddenTerm';
`;

export const GlossaryTermCreateUpdateForm: VFC = () => {
  const { t } = useTranslate();

  return (
    <Box display="grid" gap={2}>
      <TextField
        name="text"
        label={t('create_glossary_term_field_text')}
        placeholder={t('create_glossary_term_field_text_placeholder')}
        data-cy="create-glossary-term-field-text"
      />
      <TextField
        name="description"
        label={t('create_glossary_term_field_description')}
        placeholder={t('create_glossary_term_field_description_placeholder')}
        data-cy="create-glossary-term-field-description"
        multiline
        rows={4}
      />
      <StyledContainer>
        <FormControlLabel
          control={<Checkbox name="flagNonTranslatable" />}
          label={t('create_glossary_term_field_non_translatable')}
          sx={{ gridArea: 'flagNonTranslatable' }}
        />
        <FormControlLabel
          control={<Checkbox name="flagCaseSensitive" />}
          label={t('create_glossary_term_field_case_sensitive')}
          sx={{ gridArea: 'flagCaseSensitive' }}
        />
        <FormControlLabel
          control={<Checkbox name="flagAbbreviation" />}
          label={t('create_glossary_term_field_abbreviation')}
          sx={{ gridArea: 'flagAbbreviation' }}
        />
        <FormControlLabel
          control={<Checkbox name="flagForbiddenTerm" />}
          label={t('create_glossary_term_field_forbidden')}
          sx={{ gridArea: 'flagForbiddenTerm' }}
        />
      </StyledContainer>
    </Box>
  );
};
