import { Button, DialogContentText } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { getProgressData } from '../component/getProgressData';
import { GenericPlanLimitPopover } from './generic/GenericPlanLimitPopover';
import React from 'react';

type Props = {
  onClose: () => void;
  open: boolean;
};

export const PlanLimitPopoverCloud: React.FC<
  React.PropsWithChildren<Props>
> = ({ open, onClose }) => {
  const { preferredOrganization } = usePreferredOrganization();
  const { usage } = useOrganizationUsage();
  const isOwner = preferredOrganization?.currentUserRole === 'OWNER';
  const history = useHistory();

  const subscriptionLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/subscription',
    method: 'get',
    path: {
      organizationId: preferredOrganization?.id || 0,
    },
    options: {
      enabled: open && isOwner && preferredOrganization?.id !== undefined,
    },
  });

  const subscription = subscriptionLoadable.data;
  const wordsAutoUpgradeAvailable = Boolean(
    subscription &&
      subscription.plan.metricType === 'HOSTED_WORDS' &&
      !subscription.plan.free &&
      !subscription.autoUpgradeEnabled
  );

  const autoUpgradeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/auto-upgrade',
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  const handleEnableAutoUpgrade = () => {
    autoUpgradeMutation.mutate(
      {
        path: { organizationId: preferredOrganization!.id },
        content: { 'application/json': { enabled: true } },
      },
      {
        onSuccess: () => {
          onClose();
        },
      }
    );
  };

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
      additionalContent={
        wordsAutoUpgradeAvailable && (
          <DialogContentText data-cy="plan-limit-dialog-words-auto-upgrade-hint">
            <T
              keyName="plan_limit_dialog_words_auto_upgrade_hint"
              defaultValue="Your plan's word limit was reached and auto-upgrade is disabled, so adding more content is blocked. Enable auto-upgrade to move to a higher word tier automatically, or upgrade your plan manually."
            />
          </DialogContentText>
        )
      }
      actionButton={
        isOwner && (
          <>
            {wordsAutoUpgradeAvailable && (
              <LoadingButton
                data-cy="plan-limit-dialog-enable-auto-upgrade"
                color="primary"
                loading={autoUpgradeMutation.isLoading}
                onClick={handleEnableAutoUpgrade}
              >
                <T
                  keyName="plan_limit_dialog_enable_auto_upgrade"
                  defaultValue="Enable auto-upgrade"
                />
              </LoadingButton>
            )}
            <Button
              data-cy="global-confirmation-confirm"
              color="primary"
              onClick={handleConfirm}
            >
              <T keyName="plan_limit_dialog_go_to_billing" />
            </Button>
          </>
        )
      }
    />
  ) : null;
};
