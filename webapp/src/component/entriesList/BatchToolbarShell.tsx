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
  actionLabel: React.ReactNode;
  /**
   * Accessible label for the icon-only action button. Plain string so screen readers and
   * assistive tech can announce it — `actionLabel` is a `ReactNode` (typically `<T />`) so
   * cannot be used directly.
   */
  actionAriaLabel: string;
  actionDisabled?: boolean;
  actionLoading?: boolean;
  onAction: () => void;
  dataCy: string;
  actionDataCy: string;
};

export const BatchToolbarShell: React.VFC<Props> = ({
  selectionService,
  actionLabel,
  actionAriaLabel,
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
        aria-label={actionAriaLabel}
      >
        <ChevronRight width={20} height={20} />
      </LoadingButton>
    </StyledCard>
  );
};
