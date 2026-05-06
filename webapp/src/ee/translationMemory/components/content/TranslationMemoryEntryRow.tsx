import React, { useMemo } from 'react';
import { Checkbox, styled } from '@mui/material';
import { T } from '@tolgee/react';
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
  entries: TranslationMemoryEntryModel[];
  virtualEntries: VirtualTranslationMemoryEntryModel[];
};

/**
 * One renderable row produced by expanding an EntryGroup. A group with N entries per
 * (source, lang) becomes N candidate rows; each holds at most one entry per language. The
 * source/keyNames/virtualEntries decoration only renders on the primary candidate so the row
 * stack reads as "one source with multiple translation candidates".
 */
export type EntryGroupCandidate = {
  group: EntryGroup;
  candidateIndex: number;
  isPrimary: boolean;
  entriesByLang: Map<string, TranslationMemoryEntryModel>;
};

const StyledSelectionCell = styled('div')<{ $layout: EntryRowLayout }>`
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: ${({ $layout }) => ($layout === 'flat' ? '2px' : '5px')};
  ${({ $layout }) => ($layout === 'flat' ? '' : 'flex: 0 0 44px;')}
`;

type Props = {
  candidate: EntryGroupCandidate;
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
  candidate,
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
  const group = candidate.group;
  const sourceLang = languageInfo[sourceLanguageTag];
  const sourceFlag = sourceLang?.flags?.[0] || '';
  const sourceName = sourceLang?.englishName || sourceLanguageTag;

  // [candidate.entriesByLang] arrives pre-bucketed by the parent (one entry per language for
  // this candidate slot). Source-grouped buckets with multiple entries per (source, lang) are
  // expanded into multiple sibling rows upstream — each row gets one entry per language here.
  const entryByLang = candidate.entriesByLang;
  const virtualByLang = useMemo(
    () =>
      candidate.isPrimary
        ? new Map(group.virtualEntries.map((v) => [v.targetLanguageTag, v]))
        : new Map(),
    [group.virtualEntries, candidate.isPrimary]
  );

  const hasStoredEntries = group.entries.length > 0;
  const selectable = hasStoredEntries && groupId !== undefined;

  const rowEditDisabledReason: React.ReactNode = !canManage ? (
    <T
      keyName="tm_entry_edit_disabled_no_permission"
      defaultValue="Only organization maintainers can edit translation memory entries."
    />
  ) : undefined;

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
        {/* Source/keys only on the primary candidate so a multi-candidate stack reads as
            "one source, N variants" instead of repeating the source line on every row. */}
        {candidate.isPrimary && group.keyNames.length > 0 && (
          <StyledKeyName data-cy="tm-entry-row-keys">
            {group.keyNames.slice(0, 3).join(', ')}
            {group.keyNames.length > 3 &&
              `, +${group.keyNames.length - 3} more`}
          </StyledKeyName>
        )}
        <StyledSourceText>
          {candidate.isPrimary && layout === 'stacked' && (
            <StyledLanguage style={{ padding: '0' }}>
              <FlagImage flagEmoji={sourceFlag} height={16} />
              <div style={{ fontWeight: 'bold' }}>{sourceName}</div>
            </StyledLanguage>
          )}
          {candidate.isPrimary && (
            <LimitedHeightText
              maxLines={3}
              wrap="break-word"
              lineHeight="1.3em"
            >
              <TmEntryText text={group.sourceText} locale={sourceLanguageTag} />
            </LimitedHeightText>
          )}
        </StyledSourceText>
      </StyledKeyCell>

      <StyledTranslations $layout={layout}>
        {displayLanguages.map((langTag) => {
          const storedEntry = entryByLang.get(langTag);
          const virtualEntry = virtualByLang.get(langTag);
          const isEditing = editingLang === langTag;

          // Cells with a stored entry (or no entry yet) are editable when the user can manage.
          // Pure virtual cells (only virtualText) stay read-only — see TranslationCell.
          const editable = canManage;

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
              editDisabledReason={rowEditDisabledReason}
              layout={layout}
            />
          );
        })}
      </StyledTranslations>
    </StyledRow>
  );
};
