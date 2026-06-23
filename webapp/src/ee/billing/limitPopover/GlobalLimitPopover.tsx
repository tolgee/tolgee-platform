import { useOrganizationUsage } from 'tg.globalContext/helpers';
import React, { useEffect, useState } from 'react';
import { PlanLimitPopover } from './PlanLimitPopover';
import { PlanLimitPopoverSpendingLimitExceeded } from './PlanLimitPopoverSpendingLimitExceeded';

export const GlobalLimitPopover: React.FC = () => {
  const { planLimitErrors, spendingLimitErrors, mtCreditSpendingLimitErrors } =
    useOrganizationUsage();
  const [planLimitErrOpen, setPlanLimitErrOpen] = useState(false);
  const [spendingLimitErrOpen, setSpendingLimitErrOpen] = useState(false);

  useEffect(() => {
    if (planLimitErrors > 0) {
      setPlanLimitErrOpen(true);
    }
  }, [planLimitErrors]);

  useEffect(() => {
    if (spendingLimitErrors > 0 || mtCreditSpendingLimitErrors > 0) {
      setSpendingLimitErrOpen(true);
    }
  }, [spendingLimitErrors, mtCreditSpendingLimitErrors]);

  return (
    <>
      <PlanLimitPopover
        open={planLimitErrOpen}
        onClose={() => setPlanLimitErrOpen(false)}
      />
      <PlanLimitPopoverSpendingLimitExceeded
        open={spendingLimitErrOpen}
        onClose={() => setSpendingLimitErrOpen(false)}
        showSubscriptionsLink={mtCreditSpendingLimitErrors > 0}
      />
    </>
  );
};
