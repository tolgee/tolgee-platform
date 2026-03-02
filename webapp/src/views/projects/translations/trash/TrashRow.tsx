import React, { useState } from 'react';
import { Trash01 } from '@untitled-ui/icons-react';
import {
  Button,
  Checkbox,
  IconButton,
  Menu,
  MenuItem,
  styled,
  Tooltip,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { Tag } from '../Tags/Tag';
import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { UserName } from 'tg.component/common/UserName';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { KeyCellContent } from '../KeyCellContent';
import { TranslationCellReadOnly } from '../TranslationCellReadOnly';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledRow = styled('div')`
  display: grid;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  position: relative;
`;

const StyledKeyCell = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto auto auto 1fr;
  grid-template-areas:
    'checkbox  key          '
    '.         description  '
    '.         screenshots  '
    '.         tags         '
    '.         .            ';
  position: relative;
  outline: 0;
  overflow: hidden;
  min-width: 0;
`;

const StyledNamespaceChip = styled('div')`
  display: flex;
  align-items: center;
  cursor: pointer;
  background: ${({ theme }) => theme.palette.background.default};
  padding: ${({ theme }) => theme.spacing(0, 1.5, 0, 1.5)};
  padding-bottom: 1px;
  height: 24px;
  position: absolute;
  top: -12px;
  left: 2px;
  border-radius: 12px;
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 0px 7px -1px #000000'
      : '0px 0px 7px -2px #00000097'};
  z-index: 1;
  max-width: 100%;
  font-size: 14px;
  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[50]};
  }
`;

const StyledNamespaceContent = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledMoreArrow = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 2px;
  margin-right: ${({ theme }) => theme.spacing(-0.5)};
`;

const StyledCheckbox = styled(Checkbox)`
  grid-area: checkbox;
  width: 38px;
  height: 38px;
  margin: 3px -9px -9px 3px;
`;

const StyledScreenshots = styled('div')`
  grid-area: screenshots;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  padding: 0px 12px 8px 12px;
  overflow: hidden;
`;

const StyledScreenshotBox = styled('div')`
  overflow: hidden;
  position: relative;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.tokens.text._states.selected};

  &::after {
    content: '';
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    border-radius: 4px;
    border: 1px solid ${({ theme }) => theme.palette.tokens.border.primary};
    pointer-events: none;
  }
`;

const StyledScreenshotImg = styled('img')`
  width: 100%;
  height: 100%;
  object-fit: contain;
`;

const StyledTags = styled('div')`
  grid-area: tags;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  min-width: 0;
  & > * {
    margin: 0 6px 3px 0;
  }
  margin: 0px 12px 12px 12px;
  position: relative;
`;

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
  data: any;
  selected: boolean;
  onToggle: () => void;
  onRestore: () => void;
  onDelete: () => void;
  canRestore: boolean;
  canDelete: boolean;
  languages: LanguageModel[];
  columnSizes: string[];
  showNamespace: boolean;
  onFilterNamespace?: (namespace: string) => void;
};

export const TrashRow: React.FC<Props> = React.memo(function TrashRow({
  data,
  selected,
  onToggle,
  onRestore,
  onDelete,
  canRestore,
  canDelete,
  languages,
  columnSizes,
  showNamespace,
  onFilterNamespace,
}) {
  const { t } = useTranslate();
  const project = useProject();
  const formatDate = useDateFormatter();
  const [nsMenuAnchor, setNsMenuAnchor] = useState<HTMLElement | null>(null);

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

  const translations = data.translations ?? {};
  const tags = data.tags ?? [];

  return (
    <StyledRow
      style={{
        gridTemplateColumns: columnSizes.join(' '),
        width: `calc(${columnSizes.join(' + ')})`,
      }}
      data-cy="trash-row"
    >
      {showNamespace && (
        <>
          <StyledNamespaceChip
            onClick={(e) => setNsMenuAnchor(e.currentTarget)}
          >
            <StyledNamespaceContent>{data.namespace}</StyledNamespaceContent>
            <StyledMoreArrow>
              <ArrowDropDown fontSize="small" />
            </StyledMoreArrow>
          </StyledNamespaceChip>
          {nsMenuAnchor && (
            <Menu
              anchorEl={nsMenuAnchor}
              open={Boolean(nsMenuAnchor)}
              onClose={() => setNsMenuAnchor(null)}
            >
              <MenuItem
                onClick={() => {
                  onFilterNamespace?.(data.namespace);
                  setNsMenuAnchor(null);
                }}
              >
                {t('namespace_menu_filter', { namespace: data.namespace })}
              </MenuItem>
            </Menu>
          )}
        </>
      )}
      <StyledKeyCell style={showNamespace ? { paddingTop: 14 } : undefined}>
        <StyledCheckbox
          size="small"
          checked={selected}
          onChange={onToggle}
          data-cy="trash-row-checkbox"
        />
        <KeyCellContent keyName={data.name} description={data.description} />
        {data.screenshots?.length > 0 && (
          <StyledScreenshots>
            {data.screenshots.map((sc: any) => {
              const w = 100;
              const h =
                sc.width && sc.height
                  ? Math.min(w / (sc.width / sc.height), 100)
                  : 100;
              return (
                <StyledScreenshotBox
                  key={sc.id}
                  style={{ width: w, height: h }}
                >
                  <StyledScreenshotImg src={sc.thumbnailUrl} alt="" />
                </StyledScreenshotBox>
              );
            })}
          </StyledScreenshots>
        )}
        {tags.length > 0 && (
          <StyledTags>
            {tags.map((tag: any) => (
              <Tag key={tag.id} name={tag.name} />
            ))}
          </StyledTags>
        )}
      </StyledKeyCell>

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

      {languages.map((language) => {
        const translation = translations[language.tag];
        return (
          <TranslationCellReadOnly
            key={language.tag}
            text={translation?.text}
            state={translation?.state}
            locale={language.tag}
            isPlural={data.isPlural ?? false}
          />
        );
      })}
    </StyledRow>
  );
});
