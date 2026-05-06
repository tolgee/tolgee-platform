import React from 'react';
import {
  Card,
  Checkbox,
  MenuItem,
  Select,
  styled,
  Typography,
} from '@mui/material';
import { ChevronRight } from '@untitled-ui/icons-react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SelectionService } from 'tg.service/useSelectionService';

const StyledCard = styled(Card)`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  flex-direction: row;
  gap: ${({ theme }) => theme.spacing(1.5)};
  padding: ${({ theme }) => theme.spacing(1, 1.5)};
  margin: ${({ theme }) => theme.spacing(2, 1)};
  margin-left: ${({ theme }) => theme.spacing(5)};
  border-radius: ${({ theme }) => theme.spacing(1)};
  background-color: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? theme.palette.emphasis[200]
      : theme.palette.emphasis[50]};
  transition: background-color 300ms ease-in-out, visibility 0ms;
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
`;

const StyledCheckbox = styled(Checkbox)`
  margin: ${({ theme }) => theme.spacing(0, -1.5, 0, -1)};
`;

type Props = {
  selectionService: SelectionService<number>;
  /** Single-action menu label — e.g. "Delete entries". */
  actionLabel: React.ReactNode;
  /** Disable when the user lacks permission. */
  actionDisabled?: boolean;
  /** Mutation in flight — drives the LoadingButton spinner alongside selectionService.isLoading. */
  actionLoading?: boolean;
  onAction: () => void;
  dataCy: string;
  actionDataCy: string;
};

/**
 * Shared shell for the floating "N selected" batch toolbar used by Glossary terms and TM
 * entries. Owns the visibility-when-empty toggle, select-all checkbox, count, the single
 * "action" Select (currently always "Delete X" — wired so callers can localise the label),
 * and the trailing chevron-icon LoadingButton.
 *
 * Callers wire up the actual mutation + confirmation flow externally.
 */
export const BatchToolbarShell: React.VFC<Props> = ({
  selectionService,
  actionLabel,
  actionDisabled,
  actionLoading,
  onAction,
  dataCy,
  actionDataCy,
}) => {
  return (
    <StyledCard
      data-cy={dataCy}
      sx={{
        visibility: selectionService.selected.length > 0 ? 'visible' : 'hidden',
      }}
    >
      <StyledCheckbox
        size="small"
        checked={selectionService.isAllSelected}
        onChange={selectionService.toggleSelectAll}
        indeterminate={selectionService.isSomeSelected}
        disabled={selectionService.isLoading}
      />
      <Typography>{`${selectionService.selected.length} / ${selectionService.total}`}</Typography>
      <Select variant="outlined" size="small" value={0} sx={{ minWidth: 250 }}>
        <MenuItem value={0}>{actionLabel}</MenuItem>
      </Select>
      <LoadingButton
        disableElevation
        disabled={actionDisabled}
        variant="contained"
        color="primary"
        sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40, padding: 0 }}
        loading={Boolean(actionLoading) || selectionService.isLoading}
        onClick={onAction}
        data-cy={actionDataCy}
      >
        <ChevronRight width={20} height={20} />
      </LoadingButton>
    </StyledCard>
  );
};
