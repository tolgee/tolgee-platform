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
  usageUnavailable?: boolean;
};

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: 16px;
  max-width: 500px;
`;

export const GenericPlanLimitPopover: FC<
  React.PropsWithChildren<PlanLimitPopoverProps>
> = ({
  open,
  onClose,
  isPayAsYouGo,
  progressData,
  actionButton,
  loading,
  usageUnavailable,
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
          {usageUnavailable ? (
            <span data-cy="plan-limit-dialog-organization-limit-message">
              <T
                keyName="plan_limit_dialog_organization_limit_description"
                defaultValue="The organization's plan limit has been reached. Contact the organization owner to upgrade the plan."
              />
            </span>
          ) : (
            <T keyName="plan_limit_dialog_description" />
          )}
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
