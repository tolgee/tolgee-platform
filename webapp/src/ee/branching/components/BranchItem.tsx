import { components } from 'tg.service/apiSchema.generated';
import { Box, IconButton, styled, Tooltip } from '@mui/material';
import React from 'react';
import { GitMerge, ShieldTick, Trash01 } from '@untitled-ui/icons-react';
import { DefaultBranchChip } from 'tg.component/branching/DefaultBranchChip';
import { BranchNameLink } from 'tg.ee.module/branching/components/BranchNameLink';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';

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
  onMergeInto?: (() => void) | undefined | false;
};

export const BranchItem: React.FC<Props> = ({
  branch,
  onRemove,
  onMergeInto,
}) => {
  const timeDistance = useTimeDistance();
  const formatDate = useDateFormatter();

  return (
    <StyledListItem data-cy="project-settings-branch-item">
      <StyledItemText data-cy="project-settings-branch-item-name">
        <BranchNameLink name={branch.name} />
        <Box gap={1} display="flex" alignItems="center">
          {branch.isDefault && <DefaultBranchChip />}
          {branch.isProtected && <ShieldTick height={20} width={20} />}
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
        {onMergeInto && (
          <IconButton
            data-cy="project-settings-branches-merge-into-button"
            size="medium"
            onClick={onMergeInto}
          >
            <GitMerge width={20} height={20} />
          </IconButton>
        )}
        {onRemove && (
          <IconButton
            data-cy="project-settings-branches-remove-button"
            size="small"
            onClick={() => onRemove(branch)}
          >
            <Trash01 width={20} height={20} />
          </IconButton>
        )}
      </StyledItemActions>
    </StyledListItem>
  );
};
