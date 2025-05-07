import { components } from 'tg.service/apiSchema.generated';
import { Chip, styled, useTheme } from '@mui/material';
import React from 'react';
import { PropsOf } from '@emotion/react/dist/emotion-react.cjs';
import { useTranslate } from '@tolgee/react';

type GlossaryTermModel = components['schemas']['GlossaryTermModel'];

const StyledTags = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  overflow: hidden;

  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const CustomizedTag: React.VFC<PropsOf<typeof Chip>> = (props) => {
  const theme = useTheme();
  return (
    <Chip
      style={{
        backgroundColor:
          theme.palette.tokens._components.chip.placeHolderPluralFill,
      }}
      size="small"
      {...props}
    />
  );
};

type Props = {
  term: GlossaryTermModel;
};

export const GlossaryTermTags: React.VFC<Props> = ({ term }) => {
  const { t } = useTranslate();

  const hasTags =
    term.flagNonTranslatable ||
    term.flagCaseSensitive ||
    term.flagAbbreviation ||
    term.flagForbiddenTerm;

  if (!hasTags) {
    return null;
  }

  return (
    <StyledTags>
      {term.flagNonTranslatable && (
        <CustomizedTag label={t('glossary_term_flag_non_translatable')} />
      )}
      {term.flagCaseSensitive && (
        <CustomizedTag label={t('glossary_term_flag_case_sensitive')} />
      )}
      {term.flagAbbreviation && (
        <CustomizedTag label={t('glossary_term_flag_abbreviation')} />
      )}
      {term.flagForbiddenTerm && (
        <CustomizedTag label={t('glossary_term_flag_forbidden_term')} />
      )}
    </StyledTags>
  );
};
