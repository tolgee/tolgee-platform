import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import React, { useEffect, useState } from 'react';
import { PlanLimitPopoverCloud } from './PlanLimitPopoverCloud';
import { SpendingLimitExceededPopover } from './SpendingLimitExceeded';
import { PlanLimitPopover } from './PlanLimitPopover';

export const GlobalLimitPopover: React.FC = () => {
  const { planLimitErrors, spendingLimitErrors } = useOrganizationUsage();
  const [planLimitErrOpen, setPlanLimitErrOpen] = useState(false);
  const [spendingLimitErrOpen, setSpendingLimitErrOpen] = useState(false);

  useEffect(() => {
    if (planLimitErrors > 0) {
      setPlanLimitErrOpen(true);
    }
  }, [planLimitErrors]);

  useEffect(() => {
    if (spendingLimitErrors > 0) {
      setSpendingLimitErrOpen(true);
    }
  }, [spendingLimitErrors]);

  return (
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
  );
};
