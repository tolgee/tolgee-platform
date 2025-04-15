import { Box, Checkbox, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import {
  CellLanguage,
  CellLanguageModel,
} from 'tg.views/projects/translations/TranslationsTable/CellLanguage';
import React from 'react';

const StyledHeaderRow = styled('div')`
  position: sticky;
  background: ${({ theme }) => theme.palette.background.default};
  top: 0px;
  margin-bottom: -1px;
  display: grid;
`;

const StyledHeaderCell = styled('div')`
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
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
  allTermsSelected: boolean;
  someTermsSelected: boolean;
  onToggleSelectAll: () => void;
};

export const GlossaryViewListHeader: React.VFC<Props> = ({
  selectedLanguages,
  allTermsSelected,
  someTermsSelected,
  onToggleSelectAll,
}) => {
  return (
    <StyledHeaderRow
      style={{
        gridTemplateColumns:
          'minmax(350px, 1fr)' +
          ' minmax(350px, 1fr)'.repeat(selectedLanguages?.length || 0),
      }}
    >
      <StyledHeaderCell key={0}>
        <Checkbox
          checked={allTermsSelected}
          onChange={onToggleSelectAll}
          indeterminate={someTermsSelected}
        />
        <Box>
          <T keyName="glossary_grid_term_text" />
        </Box>
      </StyledHeaderCell>
      {selectedLanguages?.map((tag, i) => {
        const languageData = languageInfo[tag];
        const language: CellLanguageModel = {
          base: false,
          flagEmoji: languageData?.flags?.[0] || '',
          name: languageData?.englishName || tag,
        };
        return (
          <StyledHeaderLanguageCell key={i + 1}>
            <CellLanguage language={language} />
          </StyledHeaderLanguageCell>
        );
      })}
    </StyledHeaderRow>
  );
};
