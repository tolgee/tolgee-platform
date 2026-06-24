import React, { useMemo } from 'react';
import { Checkbox, styled, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { KeyName } from 'tg.component/KeyName/KeyName';
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

type TranslationMemoryRowCellModel =
  components['schemas']['TranslationMemoryRowCellModel'];

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
  ${({ theme, $layout }) =>
    $layout === 'flat'
      ? `border-top: 1px solid ${theme.palette.divider1};`
      : 'flex: 0 0 44px;'}
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
  /** Stable id used for selection. Only set on editable rows; read-only rows have no id. */
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

  const rowEditable = canManage && row.editable;
  const mirroredFromProject = Boolean(row.projectId);

  // Bucket cells by target language so each column lookup is O(1).
  const cellByLang = useMemo(() => {
    const m = new Map<string, TranslationMemoryRowCellModel>();
    row.cells.forEach((c) => m.set(c.targetLanguageTag, c));
    return m;
  }, [row.cells]);

  const selectable = row.editable && groupId !== undefined;

  const rowEditDisabledReason: React.ReactNode = !canManage ? (
    <T
      keyName="tm_entry_edit_disabled_no_permission"
      defaultValue="Only organization maintainers can edit translation memory entries."
    />
  ) : mirroredFromProject ? (
    <T
      keyName="tm_entry_edit_disabled_virtual"
      defaultValue="This translation comes from project <projectLink>{projectName}</projectLink>. Edit it there."
      params={{
        projectName: row.projectName!,
        projectLink: (
          <ProjectLink
            project={{
              id: row.projectId!,
              name: row.projectName!,
            }}
          />
        ),
      }}
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
        if (!selectionService || !canManage) {
          return <StyledSelectionCell $layout={layout} />;
        }
        if (selectable) {
          return (
            <StyledSelectionCell $layout={layout}>
              <Checkbox
                size="small"
                checked={selectionService.isSelected(groupId!)}
                onChange={() => selectionService.toggle(groupId!)}
                data-cy="tm-entry-row-checkbox"
              />
            </StyledSelectionCell>
          );
        }
        return (
          <StyledSelectionCell $layout={layout}>
            <Tooltip
              title={
                <T
                  keyName="tm_entry_select_disabled_virtual"
                  defaultValue="Synced entries from a project can't be selected."
                />
              }
            >
              <span>
                <Checkbox
                  size="small"
                  disabled
                  data-cy="tm-entry-row-checkbox"
                />
              </span>
            </Tooltip>
          </StyledSelectionCell>
        );
      })()}
      <StyledKeyCell $layout={layout}>
        {/* Key reference exists only for rows mirroring a project key. Editable manual
            rows have no key of their own and stay unlabeled. */}
        {row.keyName && (
          <StyledKeyName data-cy="tm-entry-row-keys">
            <KeyName name={row.keyName} />
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
          const cell = cellByLang.get(langTag);
          const isEditing = editingLang === langTag;
          return (
            <TranslationCell
              key={langTag}
              entryId={cell?.entryId ?? undefined}
              text={cell?.targetText ?? ''}
              langTag={langTag}
              sourceText={row.sourceText}
              isEditing={isEditing}
              onEdit={rowEditable ? () => onEditStart(langTag) : undefined}
              onCancel={onEditEnd}
              onSaved={onEditEnd}
              organizationId={organizationId}
              translationMemoryId={translationMemoryId}
              editable={rowEditable}
              editDisabledReason={rowEditDisabledReason}
              layout={layout}
            />
          );
        })}
      </StyledTranslations>
    </StyledRow>
  );
};
