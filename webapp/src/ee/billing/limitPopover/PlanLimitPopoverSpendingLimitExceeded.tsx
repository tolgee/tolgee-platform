import {
  Button,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  styled,
} from '@mui/material';
import { T } from '@tolgee/react';
import { SpendingLimitExceededDescription } from 'tg.component/security/SignUp/SpendingLimitExceededDesciption';
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

export const PlanLimitPopoverSpendingLimitExceeded: React.FC<Props> = ({
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
          data-cy="spending-limit-dialog-close"
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
