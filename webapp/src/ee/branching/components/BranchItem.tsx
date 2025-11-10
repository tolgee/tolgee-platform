import { components } from 'tg.service/apiSchema.generated';
import {
  Badge,
  Box,
  IconButton,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import React from 'react';
import { GitMerge, ShieldTick, Trash01 } from '@untitled-ui/icons-react';
import { DefaultBranchChip } from 'tg.component/branching/DefaultBranchChip';
import { BranchNameLink } from 'tg.ee.module/branching/components/BranchNameLink';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';
import { T } from '@tolgee/react';
import { PointerLink } from 'tg.component/layout/QuickStartGuide/StyledComponents';

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
  onRemove: (branch: BranchModel) => void;
  onMergeInto: () => void;
  onMergeDetail: () => void;
};

export const BranchItem: React.FC<Props> = ({
  branch,
  onRemove,
  onMergeInto,
  onMergeDetail,
}) => {
  const timeDistance = useTimeDistance();
  const formatDate = useDateFormatter();

  return (
    <StyledListItem data-cy="project-settings-branch-item">
      <StyledItemText data-cy="project-settings-branch-item-name">
        <BranchNameLink name={branch.name} deleted={!branch.active} />
        <Box gap={1} display="flex" alignItems="center">
          {branch.isDefault && <DefaultBranchChip />}
          {branch.isProtected && <ShieldTick height={20} width={20} />}
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
            !branch.merge && (
              <IconButton
                data-cy="project-settings-branches-merge-into-button"
                size="medium"
                onClick={onMergeInto}
              >
                <GitMerge width={20} height={20} />
              </IconButton>
            )
          ))}
        {!branch.isDefault && onRemove && (
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
