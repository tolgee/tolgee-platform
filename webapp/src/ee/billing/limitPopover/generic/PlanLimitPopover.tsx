import React, { FC } from 'react';
import {
  Button,
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
  isPayAsYouGo: boolean;
  progressData: Partial<ProgressData>;
  actionButton?: React.ReactNode;
};

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: 16px;
  max-width: 500px;
`;

export const PlanLimitPopover: FC<PlanLimitPopoverProps> = ({
  open,
  onClose,
  isPayAsYouGo,
  progressData,
  actionButton,
}) => {
  return (
    <PlanLimitPopoverWrapper
      open={open}
      onClose={onClose}
      data-cy="spending-limit-exceeded-popover"
    >
      <DialogTitle id="alert-dialog-title">
        {<T keyName="plan_limit_dialog_title" />}
      </DialogTitle>
      <StyledDialogContent>
        <DialogContentText id="alert-dialog-description">
          <T keyName="plan_limit_dialog_description" />
        </DialogContentText>
        <UsageDetailed {...progressData} isPayAsYouGo={isPayAsYouGo} />
      </StyledDialogContent>
      <Button
        data-cy="global-confirmation-cancel"
        onClick={onClose}
        type="button"
        color="secondary"
      >
        <T keyName="plan_limit_dialog_close" />
      </Button>
      <DialogActions>{actionButton}</DialogActions>
    </PlanLimitPopoverWrapper>
  );
};
