import { Checkbox, Chip, styled, useTheme } from '@mui/material';
import { GlossaryListStyledRowCell } from 'tg.ee.module/glossary/components/GlossaryListStyledRowCell';
import clsx from 'clsx';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { PropsOf } from '@emotion/react';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import Box from '@mui/material/Box';

type GlossaryTermWithTranslationsModel =
  components['schemas']['GlossaryTermWithTranslationsModel'];

const StyledRowTermCell = styled(GlossaryListStyledRowCell)`
  grid-template-areas:
    'checkbox text'
    '.        description'
    '.        tags';
    '..       ..';
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto auto 1fr;
`;

const StyledCheckbox = styled(Checkbox)`
  grid-area: checkbox;
  margin-left: ${({ theme }) => theme.spacing(-1.5)};
  margin-top: ${({ theme }) => theme.spacing(-1.5)};
`;

const StyledText = styled(Box)`
  grid-area: text;
  overflow: hidden;
  margin-bottom: ${({ theme }) => theme.spacing(1)};
`;

const StyledDescription = styled(Box)`
  grid-area: description;
  overflow: hidden;
  margin: ${({ theme }) => theme.spacing(0.5, 0)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
`;

const StyledTags = styled('div')`
  grid-area: tags;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  overflow: hidden;

  margin: ${({ theme }) => theme.spacing(0.25, 0)};
  position: relative;

  & > * {
    margin: ${({ theme }) => theme.spacing(0.25, 0.25)};
  }
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
  item: GlossaryTermWithTranslationsModel;
  editEnabled: boolean;
  baseLanguage: string | undefined;
  checked: boolean;
  onCheckedToggle: () => void;
};

export const GlossaryListTermCell: React.VFC<Props> = ({
  item,
  editEnabled,
  baseLanguage,
  checked,
  onCheckedToggle,
}) => {
  const { t } = useTranslate();

  const baseTranslation = item.translations?.find(
    (t) => t.languageCode === baseLanguage
  );

  const handleEdit = () => {
    // TODO: edit term dialog
  };

  const hasTags =
    item.flagNonTranslatable ||
    item.flagCaseSensitive ||
    item.flagAbbreviation ||
    item.flagForbiddenTerm;

  return (
    <StyledRowTermCell
      className={clsx({
        clickable: editEnabled,
      })}
      onClick={editEnabled ? handleEdit : undefined}
    >
      <StyledCheckbox checked={checked} onChange={onCheckedToggle} />
      <StyledText>
        <LimitedHeightText maxLines={3}>
          {baseTranslation?.text}
        </LimitedHeightText>
      </StyledText>
      {item.description && (
        <StyledDescription>
          <LimitedHeightText maxLines={5}>{item.description}</LimitedHeightText>
        </StyledDescription>
      )}
      {hasTags && (
        <StyledTags>
          {item.flagNonTranslatable && (
            <CustomizedTag label={t('glossary_term_flag_non_translatable')} />
          )}
          {item.flagCaseSensitive && (
            <CustomizedTag label={t('glossary_term_flag_case_sensitive')} />
          )}
          {item.flagAbbreviation && (
            <CustomizedTag label={t('glossary_term_flag_abbreviation')} />
          )}
          {item.flagForbiddenTerm && (
            <CustomizedTag label={t('glossary_term_flag_forbidden_term')} />
          )}
        </StyledTags>
      )}
    </StyledRowTermCell>
  );
};
