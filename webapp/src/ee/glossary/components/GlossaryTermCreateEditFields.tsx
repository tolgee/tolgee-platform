import { VFC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Checkbox } from 'tg.component/common/form/fields/Checkbox';
import Box from '@mui/material/Box';
import { useTranslate } from '@tolgee/react';
import { FormControlLabel, styled, Tooltip } from '@mui/material';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    'flagNonTranslatable flagCaseSensitive'
    'flagAbbreviation    flagForbiddenTerm';
`;

export const GlossaryTermCreateEditFields: VFC = () => {
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
        <Tooltip
          title={t('create_glossary_term_tooltip_non_translatable')}
          enterDelay={250}
        >
          <FormControlLabel
            control={
              <Checkbox
                name="flagNonTranslatable"
                data-cy="create-glossary-term-flag-non-translatable"
              />
            }
            label={t('create_glossary_term_field_non_translatable')}
            sx={{ gridArea: 'flagNonTranslatable' }}
          />
        </Tooltip>
        <Tooltip
          title={t('create_glossary_term_tooltip_case_sensitive')}
          enterDelay={250}
        >
          <FormControlLabel
            control={
              <Checkbox
                name="flagCaseSensitive"
                data-cy="create-glossary-term-flag-case-sensitive"
              />
            }
            label={t('create_glossary_term_field_case_sensitive')}
            sx={{ gridArea: 'flagCaseSensitive' }}
          />
        </Tooltip>
        <Tooltip
          title={t('create_glossary_term_tooltip_abbreviation')}
          enterDelay={250}
        >
          <FormControlLabel
            control={
              <Checkbox
                name="flagAbbreviation"
                data-cy="create-glossary-term-flag-abbreviation"
              />
            }
            label={t('create_glossary_term_field_abbreviation')}
            sx={{ gridArea: 'flagAbbreviation' }}
          />
        </Tooltip>
        <Tooltip
          title={t('create_glossary_term_tooltip_forbidden')}
          enterDelay={250}
        >
          <FormControlLabel
            control={
              <Checkbox
                name="flagForbiddenTerm"
                data-cy="create-glossary-term-flag-forbidden"
              />
            }
            label={t('create_glossary_term_field_forbidden')}
            sx={{ gridArea: 'flagForbiddenTerm' }}
          />
        </Tooltip>
      </StyledContainer>
    </Box>
  );
};
