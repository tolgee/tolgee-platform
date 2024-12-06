import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import React, { useEffect, useState } from 'react';
import { PlanLimitPopover } from './PlanLimitPopover';
import { SpendingLimitExceededPopover } from './SpendingLimitExceeded';

export const GlobalLimitPopover = () => {
  const { planLimitErrors, spendingLimitErrors } = useOrganizationUsage();
  const [planLimitErrOpen, setPlanLimitErrOpen] = useState(false);
  const [spendingLimitErrOpen, setSpendingLimitErrOpen] = useState(false);

  useEffect(() => {
    if (planLimitErrors === 1) {
      setPlanLimitErrOpen(true);
    }
  }, [planLimitErrors]);

  useEffect(() => {
    if (spendingLimitErrors > 0) {
      setSpendingLimitErrOpen(true);
    }
  }, [spendingLimitErrors]);

  const { preferredOrganization } = usePreferredOrganization();

  return preferredOrganization ? (
    <>
      <PlanLimitPopover
        open={planLimitErrOpen}
        onClose={() => setPlanLimitErrOpen(false)}
      />
      <SpendingLimitExceededPopover
        open={spendingLimitErrOpen}
        onClose={() => setSpendingLimitErrOpen(false)}
      />
    </>
  ) : null;
};
