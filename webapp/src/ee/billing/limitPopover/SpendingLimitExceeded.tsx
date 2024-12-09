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
import { USAGE_ELEMENT_ID } from '../component/Usage';
import { getProgressData } from '../component/utils';
import { SpendingLimitExceededDescription } from 'tg.component/security/SignUp/SpendingLimitExceededDesciption';

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
  const { usage } = useOrganizationUsage();

  const anchorEl = document.getElementById(USAGE_ELEMENT_ID);
  const progressData = usage && getProgressData(usage);

  return progressData ? (
    <Popover
      open={open}
      onClose={onClose}
      anchorEl={open ? anchorEl : undefined}
      aria-labelledby="alert-dialog-title"
      aria-describedby="alert-dialog-description"
      data-cy="spending-limit-exceeded-popover"
      {...(anchorEl
        ? {
            anchorOrigin: { horizontal: 'right', vertical: 'bottom' },
            transformOrigin: { horizontal: 'right', vertical: 'top' },
          }
        : {
            anchorOrigin: { horizontal: 'center', vertical: 'top' },
            transformOrigin: { horizontal: 'center', vertical: 'center' },
          })}
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
    </Popover>
  ) : null;
};
