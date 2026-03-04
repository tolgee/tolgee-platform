import React from 'react';
import { Trash01 } from '@untitled-ui/icons-react';
import { Button, IconButton, styled, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { UserName } from 'tg.component/common/UserName';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { TrashedKeyModel } from './TrashRow';

const StyledTrashedCell = styled('div')`
  display: flex;
  flex-direction: column;
  padding: 12px 12px;
  gap: 6px;
  border-left: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledTrashedTime = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledDeletesIn = styled('div')`
  display: flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 12px;
`;

const StyledActions = styled('div')`
  display: flex;
  align-items: center;
  gap: 4px;
`;

type Props = {
  data: TrashedKeyModel;
  canRestore: boolean;
  canDelete: boolean;
  onRestore: () => void;
  onDelete: () => void;
};

export const TrashTrashedCell: React.FC<Props> = ({
  data,
  canRestore,
  canDelete,
  onRestore,
  onDelete,
}) => {
  const { t } = useTranslate();
  const project = useProject();
  const formatDate = useDateFormatter();

  const restoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/keys/trash/{keyId}/restore',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/keys/trash',
  });

  const deleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/keys/trash/{keyId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/keys/trash',
  });

  const handleRestore = () => {
    restoreMutation.mutate(
      {
        path: { projectId: project.id, keyId: data.id },
      },
      {
        onSuccess: onRestore,
      }
    );
  };

  const handlePermanentDelete = () => {
    confirmation({
      title: t('trash_permanent_delete_title'),
      message: t('trash_permanent_delete_confirmation'),
      onConfirm() {
        deleteMutation.mutate(
          {
            path: { projectId: project.id, keyId: data.id },
          },
          {
            onSuccess: onDelete,
          }
        );
      },
    });
  };

  const deletedAt = new Date(data.deletedAt);
  const permanentDeleteAt = new Date(data.permanentDeleteAt);
  // Round up to the next whole hour for display
  const permanentDeleteAtRounded = new Date(permanentDeleteAt);
  if (
    permanentDeleteAtRounded.getMinutes() > 0 ||
    permanentDeleteAtRounded.getSeconds() > 0
  ) {
    permanentDeleteAtRounded.setHours(
      permanentDeleteAtRounded.getHours() + 1,
      0,
      0,
      0
    );
  }
  const now = new Date();
  const daysAgo = Math.floor(
    (now.getTime() - deletedAt.getTime()) / (1000 * 60 * 60 * 24)
  );
  const msUntilDelete = Math.max(
    0,
    permanentDeleteAt.getTime() - now.getTime()
  );
  const hoursUntilDelete = msUntilDelete / (1000 * 60 * 60);
  const minutesUntilDelete = msUntilDelete / (1000 * 60);

  let deletesInLabel: React.ReactNode;
  if (hoursUntilDelete >= 24) {
    const days = Math.ceil(hoursUntilDelete / 24);
    deletesInLabel = (
      <T
        keyName="trash_deletes_in"
        params={{ days, hours: days, minutes: days }}
      />
    );
  } else if (minutesUntilDelete >= 60) {
    const hours = Math.ceil(hoursUntilDelete);
    deletesInLabel = (
      <T
        keyName="trash_deletes_in_hours"
        params={{ days: hours, hours, minutes: hours }}
      />
    );
  } else {
    const minutes = Math.max(1, Math.ceil(minutesUntilDelete));
    deletesInLabel = (
      <T
        keyName="trash_deletes_in_minutes"
        params={{ days: minutes, hours: minutes, minutes }}
      />
    );
  }

  const deletedTimeText =
    daysAgo === 0
      ? t('trash_deleted_today')
      : t('trash_deleted_ago', { days: daysAgo });

  return (
    <StyledTrashedCell>
      <StyledTrashedTime>
        {data.deletedBy && (
          <Tooltip
            title={<UserName {...data.deletedBy} />}
            disableInteractive
          >
            <div style={{ display: 'flex' }}>
              <AvatarImg
                size={20}
                owner={{
                  type: 'USER',
                  id: data.deletedBy.id,
                  name: data.deletedBy.name,
                  avatar: data.deletedBy.avatar,
                  deleted: data.deletedBy.deleted,
                }}
              />
            </div>
          </Tooltip>
        )}
        {deletedTimeText}
      </StyledTrashedTime>
      <Tooltip
        title={formatDate(permanentDeleteAtRounded, {
          dateStyle: 'long',
          timeStyle: 'short',
        })}
        disableInteractive
      >
        <StyledDeletesIn>
          <Trash01 width={14} height={14} />
          {deletesInLabel}
        </StyledDeletesIn>
      </Tooltip>
      <StyledActions>
        {canRestore && (
          <Button
            size="small"
            color="error"
            variant="outlined"
            onClick={handleRestore}
            disabled={restoreMutation.isLoading}
            data-cy="trash-restore-button"
          >
            <T keyName="trash_restore_button" />
          </Button>
        )}
        {canDelete && (
          <Tooltip title={t('trash_permanent_delete_tooltip')}>
            <IconButton
              size="small"
              color="error"
              onClick={handlePermanentDelete}
              disabled={deleteMutation.isLoading}
              data-cy="trash-permanent-delete-button"
            >
              <Trash01 width={16} height={16} />
            </IconButton>
          </Tooltip>
        )}
      </StyledActions>
    </StyledTrashedCell>
  );
};
