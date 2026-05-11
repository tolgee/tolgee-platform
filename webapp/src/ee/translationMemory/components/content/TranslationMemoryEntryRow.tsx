import React, { useMemo } from 'react';
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

type TranslationMemoryRowModel =
  components['schemas']['TranslationMemoryRowModel'];

export type { EntryRowLayout } from './TranslationMemoryEntryRow.styles';
export { flatGridColumns } from './TranslationMemoryEntryRow.styles';

export type TmRow = TranslationMemoryRowModel;

const StyledSelectionCell = styled('div')<{ $layout: EntryRowLayout }>`
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: ${({ $layout }) => ($layout === 'flat' ? '2px' : '5px')};
  ${({ $layout }) => ($layout === 'flat' ? '' : 'flex: 0 0 44px;')}
`;

type Props = {
  row: TmRow;
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
  /** Stable id used for selection. Only set on STORED rows; virtual rows have no id. */
  groupId?: number;
};

export const TranslationMemoryEntryRow: React.VFC<Props> = ({
  row,
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

  const isVirtual = row.kind === 'VIRTUAL';
  const isStored = row.kind === 'STORED';

  // Bucket the row's cells by target language so each column lookup is O(1).
  const entryByLang = useMemo(() => {
    const m = new Map<string, TranslationMemoryEntryModel>();
    row.entries.forEach((e) => m.set(e.targetLanguageTag, e));
    return m;
  }, [row.entries]);
  const virtualByLang = useMemo(() => {
    const m = new Map<string, VirtualTranslationMemoryEntryModel>();
    row.virtualEntries.forEach((v) => m.set(v.targetLanguageTag, v));
    return m;
  }, [row.virtualEntries]);

  const selectable = isStored && groupId !== undefined;

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
        // line up with stored rows (with checkbox). Without this the virtual rows would
        // shift 44px left of stored rows in stacked layout once a TM mixed both kinds.
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
        {/* Key reference is a property of virtual rows (project keys). Stored rows have
            no key of their own and stay unlabeled. */}
        {isVirtual && row.keyName && (
          <StyledKeyName data-cy="tm-entry-row-keys">
            {row.keyName}
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
            <TmEntryText text={row.sourceText} locale={sourceLanguageTag} />
          </LimitedHeightText>
        </StyledSourceText>
      </StyledKeyCell>

      <StyledTranslations $layout={layout}>
        {displayLanguages.map((langTag) => {
          const storedEntry = entryByLang.get(langTag);
          const virtualEntry = virtualByLang.get(langTag);
          const isEditing = editingLang === langTag;

          // A cell is editable when the user can manage AND the row is a STORED row.
          // Virtual rows (including empty cells on them) are read-only — the project is
          // the canonical place to add a missing translation; users add new manual
          // sources via the create-entry dialog.
          const editable = canManage && isStored;
          const cellEditDisabledReason: React.ReactNode = isVirtual ? (
            <span>
              <T
                keyName="tm_entry_edit_disabled_virtual_prefix"
                defaultValue="This translation comes from project"
              />{' '}
              <ProjectLink
                project={{
                  id: row.projectId!,
                  name: row.projectName!,
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
              sourceText={row.sourceText}
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
