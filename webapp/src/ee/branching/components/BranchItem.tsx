import { components } from 'tg.service/apiSchema.generated';
import { Box, IconButton, styled } from '@mui/material';
import React from 'react';
import { ShieldTick, XClose } from '@untitled-ui/icons-react';
import { DefaultBranchChip } from 'tg.component/branching/DefaultBranchChip';
import { BranchNameChip } from 'tg.ee.module/branching/components/BranchNameChip';
import { T } from '@tolgee/react';
import { ClipboardCopy } from 'tg.component/common/ClipboardCopy';

const StyledListItem = styled('div')`
  display: contents;
`;

const StyledListItemColumn = styled('div')`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  padding: ${({ theme }) => theme.spacing(1)};

  &:first-child {
    padding-left: ${({ theme }) => theme.spacing(2)};
  }
`;

const StyledItemText = styled(StyledListItemColumn)`
  flex-grow: 1;
  display: flex;
  align-items: center;
  gap: 4px;
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
  onRemove?: () => void;
};

export const BranchItem: React.FC<Props> = ({ branch, onRemove }) => {
  return (
    <StyledListItem data-cy="project-settings-branch-item">
      <StyledItemText data-cy="project-settings-branch-item-name">
        <BranchNameChip name={branch.name} />
        <ClipboardCopy
          tooltip={<T keyName="clipboard_copy_branch_name" />}
          value={() => branch.name}
        />
      </StyledItemText>
      <StyledItemText data-cy="project-settings-branch-item-properties">
        <Box gap={2} display="flex" alignItems="center">
          {branch.isProtected && <ShieldTick height={20} width={20} />}
          {branch.isDefault && <DefaultBranchChip />}
        </Box>
      </StyledItemText>
      <StyledItemActions>
        {onRemove && (
          <IconButton
            data-cy="project-settings-branchs-remove-button"
            size="small"
            onClick={onRemove}
          >
            <XClose />
          </IconButton>
        )}
      </StyledItemActions>
    </StyledListItem>
  );
};
