import { components } from 'tg.service/apiSchema.generated';
import {
  Badge,
  Box,
  IconButton,
  Menu,
  MenuItem,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import React, { useState } from 'react';
import { DotsVertical, GitMerge, ShieldTick } from '@untitled-ui/icons-react';
import { DefaultBranchChip } from 'tg.component/branching/DefaultBranchChip';
import { BranchNameLink } from 'tg.ee.module/branching/components/BranchNameLink';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';
import { T } from '@tolgee/react';
import { PointerLink } from 'tg.component/layout/QuickStartGuide/StyledComponents';
import { LabelHint } from 'tg.component/common/LabelHint';

const StyledListItem = styled('div')`
  display: contents;
`;

const StyledListItemColumn = styled('div')`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  padding: ${({ theme }) => theme.spacing(2)};
  min-height: 70px;

  &:first-child {
    padding-left: ${({ theme }) => theme.spacing(2)};
  }
`;

const StyledItemText = styled(StyledListItemColumn)`
  flex-grow: 1;
  display: flex;
  align-items: center;
  column-gap: ${({ theme }) => theme.spacing(1)};
  flex-wrap: wrap;
  justify-content: flex-start;
`;

const StyledItemActions = styled(StyledListItemColumn)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
`;

type BranchModel = components['schemas']['BranchModel'];

type Props = {
  branch: BranchModel;
  onRemove?: (branch: BranchModel) => void;
  onRename?: (branch: BranchModel) => void;
  onSetProtected?: (branch: BranchModel) => void;
  onMergeInto?: () => void;
  onMergeDetail: () => void;
};

export const BranchItem: React.FC<Props> = ({
  branch,
  onRemove,
  onRename,
  onSetProtected,
  onMergeInto,
  onMergeDetail,
}) => {
  const timeDistance = useTimeDistance();
  const formatDate = useDateFormatter();
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const openMenu = Boolean(menuAnchor);

  const handleMenuOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    setMenuAnchor(event.currentTarget);
  };

  const handleMenuClose = () => setMenuAnchor(null);
  const hasMenuItems =
    Boolean(onRename) ||
    (!branch.isDefault && Boolean(onRemove)) ||
    Boolean(onSetProtected);

  return (
    <StyledListItem data-cy="project-settings-branch-item">
      <StyledItemText data-cy="project-settings-branch-item-name">
        <BranchNameLink name={branch.name} deleted={!branch.active} />
        <Box gap={1} display="flex" alignItems="center">
          {branch.isDefault && <DefaultBranchChip />}
          {branch.isProtected && (
            <LabelHint
              disableInteractive={false}
              title={
                <T
                  keyName="branch_protected_tooltip"
                  params={{
                    a: (
                      <a
                        href={
                          'https://docs.tolgee.io/platform/projects_and_organizations/members'
                        }
                        target={'_blank'}
                        rel="noreferrer"
                      />
                    ),
                  }}
                />
              }
            >
              <ShieldTick
                height={20}
                width={20}
                data-cy="branch-protected-icon"
              />
            </LabelHint>
          )}
          {branch.merge?.mergedAt && (
            <PointerLink onClick={onMergeDetail}>
              <Tooltip
                title={
                  <T
                    keyName="branch_merge_merged_into_branch_full_text"
                    noWrap
                    params={{
                      b: <b />,
                      branch: branch.merge.targetBranchName,
                      date: formatDate(branch.merge.mergedAt, {
                        dateStyle: 'long',
                        timeStyle: 'short',
                      }),
                    }}
                  />
                }
              >
                <DefaultChip
                  label={
                    <Typography variant="caption">
                      <T
                        keyName="branch_merge_merged_into_branch"
                        params={{
                          b: <b />,
                          date: formatDate(branch.merge.mergedAt),
                        }}
                      />
                    </Typography>
                  }
                />
              </Tooltip>
            </PointerLink>
          )}
        </Box>
      </StyledItemText>
      <StyledItemText data-cy="project-settings-branch-item-properties">
        {branch.author && (
          <Tooltip title={branch.author.name}>
            <span>
              <AvatarImg
                owner={{
                  name: branch.author.name,
                  avatar: branch.author.avatar,
                  type: 'USER',
                  id: branch.author.id || 0,
                }}
                size={32}
              />
            </span>
          </Tooltip>
        )}
        {branch.createdAt && !branch.isDefault && (
          <Tooltip
            title={formatDate(new Date(branch.createdAt), {
              dateStyle: 'long',
              timeStyle: 'short',
            })}
          >
            <span>{timeDistance(branch.createdAt)}</span>
          </Tooltip>
        )}
      </StyledItemText>
      <StyledItemActions>
        {!branch.isDefault &&
          onMergeInto &&
          (branch.merge && !branch.merge.mergedAt ? (
            <Tooltip title={<T keyName="branch_merge_into_warning_tooltip" />}>
              <Badge variant="dot" color="warning">
                <IconButton
                  data-cy="project-settings-branches-merge-into-button"
                  size="medium"
                  onClick={onMergeInto}
                >
                  <GitMerge width={20} height={20} />
                </IconButton>
              </Badge>
            </Tooltip>
          ) : (
            branch.active && (
              <Tooltip
                title={
                  <T
                    keyName="branch_merge_into_tooltip"
                    params={{ target: branch.originBranchName!, b: <b /> }}
                  />
                }
              >
                <IconButton
                  data-cy="project-settings-branches-merge-into-button"
                  size="medium"
                  onClick={onMergeInto}
                >
                  <GitMerge width={20} height={20} />
                </IconButton>
              </Tooltip>
            )
          ))}

        {hasMenuItems && (
          <>
            <IconButton
              data-cy="project-settings-branches-actions-menu"
              size="small"
              onClick={handleMenuOpen}
            >
              <DotsVertical width={20} height={20} />
            </IconButton>
            <Menu
              anchorEl={menuAnchor}
              open={openMenu}
              onClose={handleMenuClose}
            >
              {onRename && branch.active && (
                <MenuItem
                  data-cy="project-settings-branches-rename-button"
                  onClick={() => {
                    handleMenuClose();
                    onRename(branch);
                  }}
                >
                  <T keyName="project_branch_rename" />
                </MenuItem>
              )}
              {onSetProtected && branch.active && (
                <MenuItem
                  data-cy="project-settings-branches-protection-button"
                  data-cy-protect={branch.isProtected ? 'false' : 'true'}
                  onClick={() => {
                    handleMenuClose();
                    onSetProtected(branch);
                  }}
                >
                  {branch.isProtected ? (
                    <T keyName="project_branch_unprotect" />
                  ) : (
                    <T keyName="project_branch_protect" />
                  )}
                </MenuItem>
              )}
              {!branch.isDefault && onRemove && (
                <MenuItem
                  data-cy="project-settings-branches-remove-button"
                  onClick={() => {
                    handleMenuClose();
                    onRemove(branch);
                  }}
                >
                  <Typography color={(theme) => theme.palette.error.main}>
                    <T keyName="branch_merges_delete" />
                  </Typography>
                </MenuItem>
              )}
            </Menu>
          </>
        )}
      </StyledItemActions>
    </StyledListItem>
  );
};
