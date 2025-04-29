import { Button } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { getProgressData } from '../component/getProgressData';
import { GenericPlanLimitPopover } from './generic/GenericPlanLimitPopover';
import React from 'react';

type Props = {
  onClose: () => void;
  open: boolean;
};

export const PlanLimitPopoverCloud: React.FC<Props> = ({ open, onClose }) => {
  const { preferredOrganization } = usePreferredOrganization();
  const { usage } = useOrganizationUsage();
  const isOwner = preferredOrganization?.currentUserRole === 'OWNER';
  const history = useHistory();

  const handleConfirm = () => {
    onClose();
    history.push(
      LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization!.slug,
      })
    );
  };

  const progressData = usage && getProgressData({ usage });

  return progressData ? (
    <GenericPlanLimitPopover
      onClose={onClose}
      open={open}
      isPayAsYouGo={usage?.isPayAsYouGo}
      progressData={progressData}
      actionButton={
        isOwner && (
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
