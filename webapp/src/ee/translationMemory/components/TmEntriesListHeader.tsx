import { Checkbox, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { SelectionService } from 'tg.service/useSelectionService';
import { flatGridColumns } from './TranslationMemoryEntryRow';

const StyledListHeader = styled('div')`
  position: sticky;
  top: 0;
  z-index: 2;
  background: ${({ theme }) => theme.palette.background.default};
  display: grid;
  margin-bottom: -1px;
`;

const StyledListHeaderCell = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: ${({ theme }) => theme.spacing(1, 1.5)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledSelectionCell = styled(StyledListHeaderCell)`
  padding: ${({ theme }) => theme.spacing(0, 0.5)};
  justify-content: center;
`;

const StyledDataCell = styled(StyledListHeaderCell)`
  border-left: 1px solid ${({ theme }) => theme.palette.divider1};
`;

type Props = {
  sourceLanguageTag: string;
  displayLanguages: string[];
  selectionService?: SelectionService<number>;
};

export const TmEntriesListHeader: React.VFC<Props> = ({
  sourceLanguageTag,
  displayLanguages,
  selectionService,
}) => (
  <StyledListHeader
    style={{ gridTemplateColumns: flatGridColumns(displayLanguages.length) }}
    data-cy="tm-entries-header"
  >
    <StyledSelectionCell>
      {selectionService && (
        <Checkbox
          size="small"
          checked={selectionService.isAllSelected}
          indeterminate={selectionService.isSomeSelected}
          disabled={selectionService.isLoading}
          onChange={selectionService.toggleSelectAll}
          data-cy="tm-entries-select-all"
        />
      )}
    </StyledSelectionCell>
    <StyledDataCell>
      {languageInfo[sourceLanguageTag]?.flags?.[0] && (
        <FlagImage
          flagEmoji={languageInfo[sourceLanguageTag]!.flags![0]}
          height={16}
        />
      )}
      <span style={{ fontWeight: 500 }}>
        {languageInfo[sourceLanguageTag]?.englishName || sourceLanguageTag}
      </span>
      <Typography variant="caption" color="text.secondary">
        <T keyName="translation_memory_header_source" defaultValue="Source" />
      </Typography>
    </StyledDataCell>
    {displayLanguages.map((tag) => {
      const info = languageInfo[tag];
      return (
        <StyledDataCell key={tag}>
          {info?.flags?.[0] && (
            <FlagImage flagEmoji={info.flags[0]} height={16} />
          )}
          <span>{info?.englishName || tag}</span>
        </StyledDataCell>
      );
    })}
  </StyledListHeader>
);
