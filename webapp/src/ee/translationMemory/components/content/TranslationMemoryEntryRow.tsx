import React from 'react';
import { Checkbox, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { components } from 'tg.service/apiSchema.generated';
import { SelectionService } from 'tg.service/useSelectionService';
import { ProjectLink } from 'tg.component/ProjectLink';
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
  virtualsByLang: Map<string, VirtualTranslationMemoryEntryModel>;
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
  const virtualByLang = candidate.virtualsByLang;

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
        // Always reserve the selection column in both layouts so virtual rows (no checkbox)
        // line up with manual rows (with checkbox). Without this the virtual rows shifted
        // 44px left of manual rows in stacked layout once a TM mixed both kinds.
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
        {/* Per-candidate key reference. Virtual candidates show the specific project key
            they originate from; non-primary stored candidates have no key info, so we fall
            back to the aggregated group.keyNames only on the primary candidate. */}
        {(() => {
          const candidateVirtual = Array.from(
            candidate.virtualsByLang.values()
          )[0];
          if (candidateVirtual) {
            return (
              <StyledKeyName data-cy="tm-entry-row-keys">
                {candidateVirtual.keyName}
              </StyledKeyName>
            );
          }
          if (candidate.isPrimary && group.keyNames.length > 0) {
            return (
              <StyledKeyName data-cy="tm-entry-row-keys">
                {group.keyNames.slice(0, 3).join(', ')}
                {group.keyNames.length > 3 &&
                  `, +${group.keyNames.length - 3} more`}
              </StyledKeyName>
            );
          }
          return null;
        })()}
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

          // Pure virtual cells (only virtualText, no stored entry) are read-only — they reflect
          // a project translation, so the project is the right place to change them.
          const isPureVirtual = !storedEntry && virtualEntry !== undefined;
          const editable = canManage && !isPureVirtual;
          const cellEditDisabledReason: React.ReactNode = isPureVirtual ? (
            <span>
              <T
                keyName="tm_entry_edit_disabled_virtual_prefix"
                defaultValue="This translation comes from project"
              />{' '}
              <ProjectLink
                project={{
                  id: virtualEntry!.projectId,
                  name: virtualEntry!.projectName,
                }}
              />
              {'. '}
              <T
                keyName="tm_entry_edit_disabled_virtual_suffix"
                defaultValue="Edit it there."
              />
            </span>
          ) : (
            rowEditDisabledReason
          );

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
              editDisabledReason={cellEditDisabledReason}
              layout={layout}
            />
          );
        })}
      </StyledTranslations>
    </StyledRow>
  );
};
