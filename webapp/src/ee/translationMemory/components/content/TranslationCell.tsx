import React from 'react';
import { styled, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { useQueryClient } from 'react-query';
import { Edit02 } from '@untitled-ui/icons-react';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { EditableTextCellForm } from 'tg.component/entriesList/EditableTextCellForm';
import { TmEntryText } from './TmEntryText';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import {
  EntryRowLayout,
  StyledEditAffordance,
  StyledLanguage,
  StyledTranslation,
  StyledTranslationCell,
} from './TranslationMemoryEntryRow.styles';

type TranslationMemoryEntryModel =
  components['schemas']['TranslationMemoryEntryModel'];

// Wrapper around the textarea + button row so the buttons sit one full theme-spacing(2)
// (16px) below the textarea — matches Glossary's StyledEditBox layout.
const StyledEditBox = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
  flex-flow: column;
`;

type Props = {
  entry?: TranslationMemoryEntryModel;
  /**
   * Read-only target text for virtual cells — used when the row is derived from a project
   * translation rather than a stored entry. When provided and [entry] is undefined, the cell
   * renders the virtual text without any edit affordance.
   */
  virtualText?: string;
  langTag: string;
  sourceText: string;
  isEditing: boolean;
  onEdit?: () => void;
  onCancel: () => void;
  onSaved: () => void;
  organizationId: number;
  translationMemoryId: number;
  canManage?: boolean;
  /**
   * Tooltip text shown when the cell is not editable. Falsy = no tooltip. Wired by the row
   * which knows whether the disablement is a permission issue or a synced/virtual row.
   */
  editDisabledReason?: React.ReactNode;
  layout: EntryRowLayout;
};

export const TranslationCell: React.VFC<Props> = ({
  entry,
  virtualText,
  langTag,
  sourceText,
  isEditing,
  onEdit,
  onCancel,
  onSaved,
  organizationId,
  translationMemoryId,
  canManage = true,
  editDisabledReason,
  layout,
}) => {
  const queryClient = useQueryClient();
  const targetLang = languageInfo[langTag];
  const targetFlag = targetLang?.flags?.[0] || '';
  const targetName = targetLang?.englishName || langTag;

  const [value, setValue] = React.useState(entry?.targetText || '');

  const invalidate = () =>
    // Narrow to just this TM's entries — the broader prefix would also invalidate the org TM
    // list, every other TM, and unrelated metadata, forcing collateral re-fetches on every
    // cell save.
    queryClient.invalidateQueries(
      '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries'
    );

  const updateMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries/{entryId}',
    method: 'put',
  });

  const createMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries',
    method: 'post',
  });

  const deleteMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries/{entryId}',
    method: 'delete',
  });

  const handleEdit = () => {
    if (!onEdit) return;
    setValue(entry?.targetText || '');
    onEdit();
  };

  const save = () => {
    const callbacks = {
      onSuccess: () => {
        invalidate();
        onSaved();
      },
      onError: () =>
        messageService.error(
          <T
            keyName="translation_memory_save_entry_error"
            defaultValue="Failed to save entry"
          />
        ),
    };
    // Empty value clears the cell. For a stored entry that means deleting the row;
    // for a never-saved virtual cell there is nothing to do.
    if (!value.trim()) {
      if (entry) {
        deleteMutation.mutate(
          {
            path: { organizationId, translationMemoryId, entryId: entry.id },
          },
          callbacks
        );
        return;
      }
      onSaved();
      return;
    }
    const body = {
      content: {
        'application/json': {
          sourceText,
          targetLanguageTag: langTag,
          targetText: value,
        },
      },
    };
    if (entry) {
      updateMutation.mutate(
        {
          path: { organizationId, translationMemoryId, entryId: entry.id },
          ...body,
        },
        callbacks
      );
      return;
    }
    createMutation.mutate(
      {
        path: { organizationId, translationMemoryId },
        ...body,
      },
      callbacks
    );
  };

  const isSaving =
    updateMutation.isLoading ||
    createMutation.isLoading ||
    deleteMutation.isLoading;

  // Show the tooltip only on the read-only state of an un-editable cell. Editable cells get
  // the pencil-icon affordance instead; the editing state shows form controls and shouldn't
  // be obscured.
  const tooltipTitle: React.ReactNode =
    !canManage && !isEditing && editDisabledReason ? editDisabledReason : '';

  return (
    <Tooltip title={tooltipTitle} placement="bottom">
      <StyledTranslationCell
        $layout={layout}
        className={isEditing ? 'editing' : ''}
        onClick={!isEditing && canManage ? handleEdit : undefined}
        style={!canManage ? { cursor: 'default' } : undefined}
        data-cy="tm-entry-translation-cell"
      >
        {canManage && !isEditing && (
          <StyledEditAffordance
            size="small"
            className="tm-edit-affordance"
            data-cy="tm-entry-edit-affordance"
            tabIndex={-1}
          >
            <Edit02 />
          </StyledEditAffordance>
        )}
        {layout === 'stacked' && (
          <StyledLanguage>
            <FlagImage flagEmoji={targetFlag} height={16} />
            <div>{targetName}</div>
          </StyledLanguage>
        )}
        <StyledTranslation $layout={layout}>
          {!isEditing ? (
            (entry?.targetText || virtualText) && (
              <LimitedHeightText
                maxLines={3}
                wrap="break-word"
                lineHeight="1.3em"
              >
                <TmEntryText
                  text={entry?.targetText ?? virtualText ?? ''}
                  locale={langTag}
                />
              </LimitedHeightText>
            )
          ) : (
            <StyledEditBox>
              <EditableTextCellForm
                value={value}
                onChange={setValue}
                onSave={save}
                onCancel={onCancel}
                saving={isSaving}
                fieldDataCy="tm-entry-edit-field"
                cancelDataCy="tm-entry-cancel"
                saveDataCy="tm-entry-save"
              />
            </StyledEditBox>
          )}
        </StyledTranslation>
      </StyledTranslationCell>
    </Tooltip>
  );
};
