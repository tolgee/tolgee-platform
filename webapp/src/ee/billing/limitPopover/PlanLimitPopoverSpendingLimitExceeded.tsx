import {
  Button,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  styled,
} from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
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
  showSubscriptionsLink?: boolean;
};

export const PlanLimitPopoverSpendingLimitExceeded: React.FC<Props> = ({
  open,
  onClose,
  showSubscriptionsLink,
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const isOwner = preferredOrganization?.currentUserRole === 'OWNER';
  const history = useHistory();

  const handleGoToSubscriptions = () => {
    onClose();
    history.push(
      LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization!.slug,
      })
    );
  };

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
        {isOwner && showSubscriptionsLink && (
          <Button
            data-cy="spending-limit-dialog-go-to-subscriptions"
            color="primary"
            onClick={handleGoToSubscriptions}
          >
            <T
              keyName="spending_limit_dialog_go_to_subscriptions"
              defaultValue="Go to subscriptions"
            />
          </Button>
        )}
      </DialogActions>
    </PlanLimitPopoverWrapper>
  );
};
