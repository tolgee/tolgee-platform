import { IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { FC } from 'react';
import { DotsVertical } from '@untitled-ui/icons-react';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  useIsOrganizationOwnerOrMaintainer,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { messageService } from 'tg.service/MessageService';
import { TranslationMemorySettingsDialog } from 'tg.ee.module/translationMemory/views/TranslationMemorySettingsDialog';
import { TmWriteOnlyReviewedDialog } from 'tg.ee.module/translationMemory/views/TmWriteOnlyReviewedDialog';

type Props = {
  translationMemory: {
    id: number;
    name: string;
    type: string;
    sourceLanguageTag: string;
    writeOnlyReviewed?: boolean;
  };
};

export const TranslationMemoryListItemMenu: FC<Props> = ({
  translationMemory,
}) => {
  const { preferredOrganization } = usePreferredOrganization();

  const { t } = useTranslate();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [isEditing, setIsEditing] = React.useState(false);

  const canManage = useIsOrganizationOwnerOrMaintainer();
  const isProjectTm = translationMemory.type === 'PROJECT';

  const deleteMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const writeOnlyReviewedMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/write-only-reviewed',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const onDelete = () => {
    setAnchorEl(null);
    confirmation({
      title: (
        <T
          keyName="delete_translation_memory_confirmation_title"
          defaultValue="Delete translation memory"
        />
      ),
      message: (
        <T
          keyName="delete_translation_memory_confirmation_message"
          defaultValue="This will permanently delete this translation memory and all its entries. This action cannot be undone."
        />
      ),
      hardModeText: translationMemory.name.toUpperCase(),
      onConfirm() {
        deleteMutation.mutate({
          path: {
            organizationId: preferredOrganization!.id,
            translationMemoryId: translationMemory.id,
          },
        });
      },
    });
  };

  if (!canManage) {
    return null;
  }

  return (
    <>
      <Tooltip
        title={t('translation_memories_list_more_button', 'More options')}
      >
        <IconButton
          onClick={(e) => {
            e.stopPropagation();
            handleOpen(e);
          }}
          data-cy="translation-memories-list-more-button"
          aria-label={t(
            'translation_memories_list_more_button',
            'More options'
          )}
          size="small"
        >
          <DotsVertical />
        </IconButton>
      </Tooltip>

      <Menu
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        id="translation-memory-item-menu"
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        onClick={stopBubble()}
      >
        <MenuItem
          data-cy="translation-memory-edit-button"
          onClick={() => {
            setAnchorEl(null);
            setIsEditing(true);
          }}
        >
          <T
            keyName="translation_memory_settings_button"
            defaultValue="Settings"
          />
        </MenuItem>
        {!isProjectTm && (
          <MenuItem
            data-cy="translation-memory-delete-button"
            onClick={onDelete}
            sx={{ color: 'error.main' }}
          >
            <T
              keyName="translation_memory_delete_button"
              defaultValue="Delete"
            />
          </MenuItem>
        )}
      </Menu>

      {isEditing && !isProjectTm && (
        <div onClick={(e) => e.stopPropagation()}>
          <TranslationMemorySettingsDialog
            translationMemoryId={translationMemory.id}
            open={isEditing}
            onClose={() => setIsEditing(false)}
            onFinished={() => setIsEditing(false)}
          />
        </div>
      )}

      {isEditing && isProjectTm && (
        <div onClick={(e) => e.stopPropagation()}>
          <TmWriteOnlyReviewedDialog
            open={isEditing}
            initialWriteOnlyReviewed={
              translationMemory.writeOnlyReviewed ?? false
            }
            saving={writeOnlyReviewedMutation.isLoading}
            onClose={() => setIsEditing(false)}
            onSave={(writeOnlyReviewed) =>
              writeOnlyReviewedMutation.mutate(
                {
                  path: {
                    organizationId: preferredOrganization!.id,
                    translationMemoryId: translationMemory.id,
                  },
                  content: { 'application/json': { writeOnlyReviewed } },
                },
                {
                  onSuccess: () => setIsEditing(false),
                  onError: () =>
                    messageService.error('Failed to save settings'),
                }
              )
            }
          />
        </div>
      )}
    </>
  );
};
