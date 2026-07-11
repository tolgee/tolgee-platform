import { Box, Checkbox, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import {
  CellLanguage,
  CellLanguageModel,
} from 'tg.views/projects/translations/TranslationsTable/CellLanguage';
import React from 'react';
import { SelectionService } from 'tg.service/useSelectionService';

const StyledHeaderRow = styled('div')`
  position: sticky;
  background: ${({ theme }) => theme.palette.background.default};
  z-index: ${({ theme }) => theme.zIndex.fab - 1};
  top: 0;
  margin-bottom: -1px;
  display: grid;
`;

const StyledHeaderCell = styled('div')`
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  box-sizing: border-box;
  display: flex;
  flex-grow: 0;
  align-content: center;
  align-items: center;
  overflow: hidden;
`;

const StyledHeaderLanguageCell = styled(StyledHeaderCell)`
  border-left: 1px solid ${({ theme }) => theme.palette.divider1};
`;

type Props = {
  selectedLanguages: string[] | undefined;
  selectionService: SelectionService<number>;
};

export const GlossaryViewListHeader: React.VFC<Props> = ({
  selectedLanguages,
  selectionService,
}) => {
  const { t } = useTranslate();

  return (
    <StyledHeaderRow
      style={{
        gridTemplateColumns:
          'minmax(300px, 1fr)' +
          ' minmax(200px, 1fr)'.repeat((selectedLanguages?.length || 1) - 1),
      }}
    >
      <StyledHeaderCell key={0} role="columnheader">
        <Checkbox
          size="small"
          checked={selectionService.isAllSelected}
          onChange={selectionService.toggleSelectAll}
          indeterminate={selectionService.isSomeSelected}
          disabled={selectionService.isLoading}
          aria-label={t('glossary_grid_terms_select_all')}
        />
        <Box>
          <T keyName="glossary_grid_term_text" />
        </Box>
      </StyledHeaderCell>
      {selectedLanguages?.slice(1)?.map((tag, i) => {
        const languageData = languageInfo[tag];
        const language: CellLanguageModel = {
          base: false,
          flagEmoji: languageData?.flags?.[0] || '',
          name: languageData?.englishName || tag,
        };
        return (
          <StyledHeaderLanguageCell key={i + 1} role="columnheader">
            <CellLanguage language={language} />
          </StyledHeaderLanguageCell>
        );
      })}
    </StyledHeaderRow>
  );
};
