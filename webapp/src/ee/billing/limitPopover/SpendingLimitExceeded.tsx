import {
  Button,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Popover,
  styled,
} from '@mui/material';
import { T } from '@tolgee/react';

import { useOrganizationUsage } from 'tg.globalContext/helpers';
import { USAGE_ELEMENT_ID } from '../component/CriticalUsageCircle';
import { SpendingLimitExceededDescription } from 'tg.component/security/SignUp/SpendingLimitExceededDesciption';
import { getProgressData } from '../component/getProgressData';
import { PlanLimitPopoverWrapper } from './generic/PlanLimitPopoverWrapper';

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: 16px;
  max-width: 500px;
`;

type Props = {
  onClose: () => void;
  open: boolean;
};

export const SpendingLimitExceededPopover: React.FC<Props> = ({
  open,
  onClose,
}) => {
  return (
    <PlanLimitPopoverWrapper
      open={open}
      onClose={onClose}
      data-cy="spending-limit-exceeded-popover"
    >
      <DialogTitle id="alert-dialog-title">
        {<T keyName="spending_limit_dialog_title" />}
      </DialogTitle>
      <StyledDialogContent>
        <DialogContentText id="alert-dialog-description">
          <SpendingLimitExceededDescription />
        </DialogContentText>
      </StyledDialogContent>

      <DialogActions>
        <Button
          data-cy="global-confirmation-cancel"
          onClick={onClose}
          type="button"
          color="secondary"
        >
          <T keyName="spending_limit_dialog_close" />
        </Button>
      </DialogActions>
    </PlanLimitPopoverWrapper>
  );
};
