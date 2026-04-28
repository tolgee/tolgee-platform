import React from 'react';
import { Button, TextField } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { useQueryClient } from 'react-query';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { TmEntryText } from './TmEntryText';
import { messageService } from 'tg.service/MessageService';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import {
  EntryRowLayout,
  StyledControls,
  StyledEmpty,
  StyledLanguage,
  StyledTranslation,
  StyledTranslationCell,
} from './TranslationMemoryEntryRow.styles';

type TranslationMemoryEntryModel =
  components['schemas']['TranslationMemoryEntryModel'];

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
  layout,
}) => {
  const { t } = useTranslate();
  const queryClient = useQueryClient();
  const targetLang = languageInfo[langTag];
  const targetFlag = targetLang?.flags?.[0] || '';
  const targetName = targetLang?.englishName || langTag;

  const [value, setValue] = React.useState(entry?.targetText || '');

  const invalidate = () =>
    queryClient.invalidateQueries(
      '/v2/organizations/{organizationId}/translation-memories'
    );

  const updateMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries/{entryId}',
    method: 'put',
  });

  const createMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries',
    method: 'post',
  });

  const handleEdit = () => {
    if (!onEdit) return;
    setValue(entry?.targetText || '');
    onEdit();
  };

  const save = () => {
    const body = {
      content: {
        'application/json': {
          sourceText,
          targetLanguageTag: langTag,
          targetText: value,
        },
      },
    };
    const callbacks = {
      onSuccess: () => {
        invalidate();
        onSaved();
      },
      onError: () => messageService.error('Failed to save entry'),
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

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      save();
    }
    if (e.key === 'Escape') {
      onCancel();
    }
  };

  const isSaving = updateMutation.isLoading || createMutation.isLoading;

  return (
    <StyledTranslationCell
      $layout={layout}
      className={isEditing ? 'editing' : ''}
      onClick={!isEditing && canManage ? handleEdit : undefined}
      style={!canManage ? { cursor: 'default' } : undefined}
      data-cy="tm-entry-translation-cell"
    >
      {layout === 'stacked' && (
        <StyledLanguage>
          <FlagImage flagEmoji={targetFlag} height={16} />
          <div>{targetName}</div>
        </StyledLanguage>
      )}
      <StyledTranslation $layout={layout}>
        {!isEditing ? (
          entry?.targetText || virtualText ? (
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
          ) : (
            <StyledEmpty>—</StyledEmpty>
          )
        ) : (
          <>
            <TextField
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onKeyDown={handleKeyDown}
              multiline
              minRows={2}
              fullWidth
              autoFocus
              size="small"
              data-cy="tm-entry-edit-field"
            />
            <StyledControls>
              <div style={{ flex: 1 }} />
              <Button
                onClick={onCancel}
                size="small"
                variant="outlined"
                data-cy="tm-entry-cancel"
              >
                {t('global_cancel_button')}
              </Button>
              <LoadingButton
                onClick={save}
                size="small"
                variant="contained"
                color="primary"
                loading={isSaving}
                data-cy="tm-entry-save"
              >
                {t('global_form_save')}
              </LoadingButton>
            </StyledControls>
          </>
        )}
      </StyledTranslation>
    </StyledTranslationCell>
  );
};
