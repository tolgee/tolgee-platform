import { Button } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import {
  useBillingOrganization,
  useOrganizationUsage,
} from 'tg.globalContext/helpers';
import { getProgressData } from '../component/getProgressData';
import { GenericPlanLimitPopover } from './generic/GenericPlanLimitPopover';
import React from 'react';
import { billingLinkFor } from 'tg.fixtures/billingLink';

type Props = {
  onClose: () => void;
  open: boolean;
};

export const PlanLimitPopoverCloud: React.FC<
  React.PropsWithChildren<Props>
> = ({ open, onClose }) => {
  const { usage } = useOrganizationUsage();
  const billingOrganization = useBillingOrganization();
  const history = useHistory();

  const handleConfirm = () => {
    if (!billingOrganization) {
      return;
    }
    onClose();
    history.push(billingLinkFor({ slug: billingOrganization.slug }));
  };

  const progressData = usage && getProgressData({ usage });

  return progressData ? (
    <GenericPlanLimitPopover
      onClose={onClose}
      open={open}
      isPayAsYouGo={usage?.isPayAsYouGo}
      progressData={progressData}
      actionButton={
        billingOrganization && (
          <Button
            data-cy="global-confirmation-confirm"
            color="primary"
            onClick={handleConfirm}
          >
            <T keyName="plan_limit_dialog_go_to_billing" />
          </Button>
        )
      }
    />
  ) : null;
};
