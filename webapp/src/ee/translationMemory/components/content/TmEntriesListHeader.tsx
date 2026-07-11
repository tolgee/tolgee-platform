import { Checkbox, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { SelectionService } from 'tg.service/useSelectionService';
import {
  HeaderCell,
  HeaderDataCell,
  HeaderRow,
} from 'tg.component/entriesList/headerChrome';
import { flatGridColumns } from './TranslationMemoryEntryRow';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';

const StyledDataCell = styled(HeaderDataCell)`
  gap: 8px;
  padding: ${({ theme }) => theme.spacing(1, 1.5)};
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledSelectionCell = styled(HeaderCell)`
  padding: ${({ theme }) => theme.spacing(0, 0.5)};
  justify-content: center;
`;

type Props = {
  sourceLanguageTag: string;
  displayLanguages: string[];
  selectionService?: SelectionService<number>;
  /** When false, the leading cell stays for grid alignment but renders empty. */
  canManage?: boolean;
};

export const TmEntriesListHeader: React.VFC<Props> = ({
  sourceLanguageTag,
  displayLanguages,
  selectionService,
  canManage = true,
}) => (
  <HeaderRow
    style={{ gridTemplateColumns: flatGridColumns(displayLanguages.length) }}
    data-cy="tm-entries-header"
  >
    <StyledSelectionCell>
      {selectionService && canManage && (
        <Checkbox
          size="small"
          checked={selectionService.isAllSelected}
          indeterminate={selectionService.isSomeSelected}
          disabled={selectionService.isLoading}
          // Not `toggleSelectAll`: virtual rows inflate `totalCount`, so `isAllSelected`
          // never flips on and toggle would always re-select.
          onChange={() =>
            selectionService.isAnySelected
              ? selectionService.unselectAll()
              : selectionService.selectAll()
          }
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
      <DefaultChip
        data-cy="tm-entries-header-base-badge"
        label={
          <T keyName="translation_memory_header_base" defaultValue="Base" />
        }
      />
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
  </HeaderRow>
);
