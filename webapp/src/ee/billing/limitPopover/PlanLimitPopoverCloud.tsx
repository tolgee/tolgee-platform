import { Button, DialogContentText } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { getProgressData } from '../component/getProgressData';
import { GenericPlanLimitPopover } from './generic/GenericPlanLimitPopover';
import { useWordsAutoUpgrade } from './useWordsAutoUpgrade';
import React from 'react';

type Props = {
  onClose: () => void;
  open: boolean;
};

export const PlanLimitPopoverCloud: React.FC<
  React.PropsWithChildren<Props>
> = ({ open, onClose }) => {
  const { preferredOrganization } = usePreferredOrganization();
  const { usage, planLimitErrorCode } = useOrganizationUsage();
  const isOwner = preferredOrganization?.currentUserRole === 'OWNER';
  const history = useHistory();

  const progressData = usage && getProgressData({ usage });

  // The rejected write is not persisted, so the reported usage does not show the overshoot that
  // caused it. Only the error code says which limit was hit.
  const wordsExhausted = planLimitErrorCode === 'plan_word_limit_exceeded';

  const wordsAutoUpgrade = useWordsAutoUpgrade({
    enabled: open && isOwner,
    wordsExhausted,
  });

  const handleConfirm = () => {
    onClose();
    history.push(
      LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization!.slug,
      })
    );
  };

  return progressData ? (
    <GenericPlanLimitPopover
      onClose={onClose}
      open={open}
      isPayAsYouGo={usage?.isPayAsYouGo}
      progressData={progressData}
      additionalContent={
        <>
          {wordsAutoUpgrade.available && (
            <DialogContentText data-cy="plan-limit-dialog-words-auto-upgrade-hint">
              <T
                keyName="plan_limit_dialog_words_auto_upgrade_hint"
                defaultValue="Your plan's word limit was reached and auto-upgrade is disabled, so adding more content is blocked. Enable auto-upgrade to move to a higher word tier automatically, or upgrade your plan manually."
              />
            </DialogContentText>
          )}
          {wordsAutoUpgrade.ineffective &&
            wordsAutoUpgrade.reason === 'largestTier' && (
              <DialogContentText data-cy="plan-limit-dialog-words-largest-tier">
                <T
                  keyName="plan_limit_dialog_words_largest_tier"
                  defaultValue="You're on the largest plan we sell, so auto-upgrade has nothing bigger to move to. Contact us and we'll size a plan to your volume."
                />
              </DialogContentText>
            )}
          {wordsAutoUpgrade.ineffective &&
            wordsAutoUpgrade.reason === 'scheduledChange' && (
              <DialogContentText data-cy="plan-limit-dialog-words-scheduled-change">
                <T
                  keyName="plan_limit_dialog_words_scheduled_change"
                  defaultValue="A plan change is already scheduled, so auto-upgrade is paused until it applies. Cancel the scheduled change to let auto-upgrade raise your tier."
                />
              </DialogContentText>
            )}
          {wordsAutoUpgrade.ineffective &&
            wordsAutoUpgrade.reason === 'other' && (
              <DialogContentText data-cy="plan-limit-dialog-words-auto-upgrade-ineffective">
                <T
                  keyName="plan_limit_dialog_words_auto_upgrade_ineffective"
                  defaultValue="Your plan's word limit was reached. Auto-upgrade is already on, but it cannot be applied to this subscription. Contact us to find the right plan."
                />
              </DialogContentText>
            )}
        </>
      }
      actionButton={
        isOwner && (
          <>
            {wordsAutoUpgrade.available && (
              <LoadingButton
                data-cy="plan-limit-dialog-enable-auto-upgrade"
                color="primary"
                loading={wordsAutoUpgrade.isEnabling}
                onClick={() => wordsAutoUpgrade.enable(onClose)}
              >
                <T
                  keyName="plan_limit_dialog_enable_auto_upgrade"
                  defaultValue="Enable auto-upgrade"
                />
              </LoadingButton>
            )}
            {wordsAutoUpgrade.ineffective &&
              wordsAutoUpgrade.reason === 'largestTier' && (
                <Button
                  data-cy="plan-limit-dialog-contact-us"
                  color="primary"
                  href="mailto:info@tolgee.io"
                >
                  <T
                    keyName="plan_limit_dialog_contact_us"
                    defaultValue="Contact us"
                  />
                </Button>
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
