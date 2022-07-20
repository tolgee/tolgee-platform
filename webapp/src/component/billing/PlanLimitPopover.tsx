import { styled } from '@mui/material';
import {
  Button,
  DialogActions,
  DialogContent,
  DialogTitle,
  DialogContentText,
  Popover,
} from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import {
  usePreferredOrganization,
  useOrganizationUsage,
} from 'tg.globalContext/helpers';
import { USAGE_ELEMENT_ID } from './Usage';
import { UsageDetailed } from './UsageDetailed';
import { getProgressData } from './utils';

const StyledDialogContent = styled(DialogContent)`
  display: grid;
  gap: 16px;
  max-width: 500px;
`;

type Props = {
  onClose: () => void;
  open: boolean;
};

export const PlanLimitPopover: React.FC<Props> = ({ open, onClose }) => {
  const { preferredOrganization } = usePreferredOrganization();
  const { usage } = useOrganizationUsage();
  const isOwner = preferredOrganization.currentUserRole === 'OWNER';
  const history = useHistory();

  const handleConfirm = () => {
    onClose();
    history.push(
      LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
      })
    );
  };

  const anchorEl = document.getElementById(USAGE_ELEMENT_ID);
  const progressData = usage && getProgressData(usage);

  return progressData ? (
    <Popover
      open={open}
      onClose={onClose}
      anchorEl={open ? anchorEl : undefined}
      aria-labelledby="alert-dialog-title"
      aria-describedby="alert-dialog-description"
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
        {<T keyName="plan_limit_dialog_title" />}
      </DialogTitle>
      <StyledDialogContent>
        <DialogContentText id="alert-dialog-description">
          <T keyName="plan_limit_dialog_description" />
        </DialogContentText>
        <UsageDetailed {...progressData} />
      </StyledDialogContent>

      <DialogActions>
        <Button
          data-cy="global-confirmation-cancel"
          onClick={onClose}
          type="button"
          color="secondary"
        >
          <T>plan_limit_dialog_close</T>
        </Button>
        {isOwner && (
          <Button
            data-cy="global-confirmation-confirm"
            color="primary"
            onClick={handleConfirm}
          >
            <T>plan_limit_dialog_go_to_billing</T>
          </Button>
        )}
      </DialogActions>
    </Popover>
  ) : null;
};
