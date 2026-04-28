import React from 'react';
import { Checkbox, styled } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { components } from 'tg.service/apiSchema.generated';
import { SelectionService } from 'tg.service/useSelectionService';
import { TranslationCell } from './TranslationCell';
import { TmEntryText } from './TmEntryText';
import {
  EntryRowLayout,
  flatGridColumns,
  StyledKeyCell,
  StyledKeyName,
  StyledLanguage,
  StyledRow,
  StyledSourceText,
  StyledTranslations,
} from './TranslationMemoryEntryRow.styles';

type TranslationMemoryEntryModel =
  components['schemas']['TranslationMemoryEntryModel'];

type VirtualTranslationMemoryEntryModel =
  components['schemas']['VirtualTranslationMemoryEntryModel'];

export type { EntryRowLayout } from './TranslationMemoryEntryRow.styles';
export { flatGridColumns } from './TranslationMemoryEntryRow.styles';

export type EntryGroup = {
  sourceText: string;
  keyNames: string[];
  isManual: boolean;
  entries: TranslationMemoryEntryModel[];
  virtualEntries: VirtualTranslationMemoryEntryModel[];
};

const StyledSelectionCell = styled('div')<{ $layout: EntryRowLayout }>`
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: ${({ $layout }) => ($layout === 'flat' ? '2px' : '5px')};
  ${({ $layout }) => ($layout === 'flat' ? '' : 'flex: 0 0 44px;')}
`;

type Props = {
  group: EntryGroup;
  sourceLanguageTag: string;
  displayLanguages: string[];
  organizationId: number;
  translationMemoryId: number;
  editingLang: string | null;
  onEditStart: (langTag: string) => void;
  onEditEnd: () => void;
  canManage?: boolean;
  layout?: EntryRowLayout;
  selectionService?: SelectionService<number>;
  groupId?: number;
};

export const TranslationMemoryEntryRow: React.VFC<Props> = ({
  group,
  sourceLanguageTag,
  displayLanguages,
  organizationId,
  translationMemoryId,
  editingLang,
  onEditStart,
  onEditEnd,
  canManage = true,
  layout = 'stacked',
  selectionService,
  groupId,
}) => {
  const sourceLang = languageInfo[sourceLanguageTag];
  const sourceFlag = sourceLang?.flags?.[0] || '';
  const sourceName = sourceLang?.englishName || sourceLanguageTag;

  const entryByLang = new Map(
    group.entries.map((e) => [e.targetLanguageTag, e])
  );
  const virtualByLang = new Map(
    group.virtualEntries.map((v) => [v.targetLanguageTag, v])
  );

  // Stored rows are selectable iff they carry a real entry ID; virtual rows never are.
  const hasStoredEntries = group.entries.length > 0;
  const selectable = hasStoredEntries && groupId !== undefined;

  return (
    <StyledRow
      $layout={layout}
      style={
        layout === 'flat'
          ? { gridTemplateColumns: flatGridColumns(displayLanguages.length) }
          : undefined
      }
      data-cy="translation-memory-entry-row"
    >
      {(() => {
        const hasCheckbox = Boolean(
          selectionService && selectable && canManage
        );
        // In flat layout the selection cell is part of the grid template so it must always
        // render (otherwise columns shift relative to the sticky header). In stacked layout
        // we drop the empty 44px column for non-selectable rows so the source text sits
        // closer to the row's left edge — most project-TM rows are virtual and never carry
        // a checkbox, leaving the column wastefully empty.
        if (!hasCheckbox && layout === 'stacked') return null;
        return (
          <StyledSelectionCell $layout={layout}>
            {hasCheckbox && (
              <Checkbox
                size="small"
                checked={selectionService!.isSelected(groupId!)}
                onChange={() => selectionService!.toggle(groupId!)}
                data-cy="tm-entry-row-checkbox"
              />
            )}
          </StyledSelectionCell>
        );
      })()}
      <StyledKeyCell $layout={layout}>
        {group.keyNames.length > 0 && (
          <StyledKeyName data-cy="tm-entry-row-keys">
            {group.keyNames.slice(0, 3).join(', ')}
            {group.keyNames.length > 3 &&
              `, +${group.keyNames.length - 3} more`}
          </StyledKeyName>
        )}
        <StyledSourceText>
          {layout === 'stacked' && (
            <StyledLanguage style={{ padding: '0' }}>
              <FlagImage flagEmoji={sourceFlag} height={16} />
              <div style={{ fontWeight: 'bold' }}>{sourceName}</div>
            </StyledLanguage>
          )}
          <LimitedHeightText maxLines={3} wrap="break-word" lineHeight="1.3em">
            <TmEntryText text={group.sourceText} locale={sourceLanguageTag} />
          </LimitedHeightText>
        </StyledSourceText>
      </StyledKeyCell>

      <StyledTranslations $layout={layout}>
        {displayLanguages.map((langTag) => {
          const storedEntry = entryByLang.get(langTag);
          const virtualEntry = virtualByLang.get(langTag);
          const isEditing = editingLang === langTag;

          // Editable only when the whole group is manual AND there's either a stored entry to
          // update or no entry to create. Synced (non-manual) and virtual rows fall through to
          // read-only rendering.
          const editable = canManage && group.isManual;

          return (
            <TranslationCell
              key={langTag}
              entry={storedEntry}
              virtualText={virtualEntry?.targetText}
              langTag={langTag}
              sourceText={group.sourceText}
              isEditing={isEditing}
              onEdit={editable ? () => onEditStart(langTag) : undefined}
              onCancel={onEditEnd}
              onSaved={onEditEnd}
              organizationId={organizationId}
              translationMemoryId={translationMemoryId}
              canManage={editable}
              layout={layout}
            />
          );
        })}
      </StyledTranslations>
    </StyledRow>
  );
};
