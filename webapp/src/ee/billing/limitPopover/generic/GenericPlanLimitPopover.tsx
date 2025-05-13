import React, { FC } from 'react';
import {
  Button,
  CircularProgress,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  styled,
} from '@mui/material';
import { T } from '@tolgee/react';
import { UsageDetailed } from '../../component/UsageDetailed';
import {
  PlanLimitPopoverWrapper,
  PlanLimitPopoverWrapperProps,
} from './PlanLimitPopoverWrapper';
import { ProgressData } from '../../component/getProgressData';

type PlanLimitPopoverProps = PlanLimitPopoverWrapperProps & {
  isPayAsYouGo?: boolean;
  progressData?: Partial<ProgressData>;
  actionButton?: React.ReactNode;
  loading?: boolean;
};

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: 16px;
  max-width: 500px;
`;

export const GenericPlanLimitPopover: FC<PlanLimitPopoverProps> = ({
  open,
  onClose,
  isPayAsYouGo,
  progressData,
  actionButton,
  loading,
}) => {
  return (
    <PlanLimitPopoverWrapper
      open={open}
      onClose={onClose}
      data-cy="plan-limit-exceeded-popover"
    >
      <DialogTitle id="alert-dialog-title">
        {<T keyName="plan_limit_dialog_title" />}
      </DialogTitle>
      <StyledDialogContent>
        <DialogContentText id="alert-dialog-description">
          <T keyName="plan_limit_dialog_description" />
        </DialogContentText>
        {progressData && isPayAsYouGo !== undefined ? (
          <UsageDetailed {...progressData} isPayAsYouGo={isPayAsYouGo} />
        ) : (
          loading && <CircularProgress />
        )}
      </StyledDialogContent>

      <DialogActions>
        <Button
          data-cy="plan-limit-dialog-close"
          onClick={onClose}
          type="button"
          color="secondary"
        >
          <T keyName="plan_limit_dialog_close" />
        </Button>
        {actionButton}
      </DialogActions>
    </PlanLimitPopoverWrapper>
  );
};
